package io.github.mjaroslav.reflectors.v4;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@UtilityClass
public class Reflectors {
    /**
     * Ставим true для вывода логов библиотеки.
     */
    public boolean enabledLogs;
    /**
     * Ставим true, когда мод выполняется не в dev среде.
     */
    public boolean obfuscated;

    /**
     * Отражаем целевой класс классом-отражателем. Все декларированные методы из целевого
     * класса будут заменены соответствующими методами из отражателя. Должны совпадать имена
     * методов, методы-отражатели должны быть публичными и статичными, а также содержать объект
     * целевого класса в качестве первого аргумента, если отражается не статичный метод.
     * Вы также должны добавить файл ваших маппингов в корень ресурсов с именем methods.csv или
     * methods.txt, для .txt формата используется такие строки: "{@code SRG,имя}" для каждого метода.
     *
     * @param data           байты класса для конвертации.
     * @param target         имя целевого класса.
     * @param reflectorClass имя класса-отражателя, вы можете использовать {@link Class#getName()}
     *                       без проблем, я думаю.
     * @return байты конвертированного класса.
     */
    public byte[] reflectClass(byte[] data, @NotNull String target, @NotNull String reflectorClass) {
        log("Trying reflect target class \"" + target + "\" with \"" + reflectorClass + "\" reflector class...");
        try {
            val classNode = readClassFromBytes(data);
            val reflectorClassNode = readClass(reflectorClass);
            for (var method : reflectorClassNode.methods)
                if ((method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC &&
                        (method.access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
                    log("Found method \"" + method.name + method.desc + "\" trying to replace in target class...");
                    reflectMethod(classNode, reflectorClassNode, method);
                }
            log("Reflection done!");
            return writeClassToBytes(classNode, ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        } catch (Exception e) {
            log("Error while class reflecting:");
            e.printStackTrace();
            return data;
        }
    }

    // ====================
    // Общие утилиты
    // ====================

    /**
     * Логирование, зависимое от значения {@link Reflectors#enabledLogs}.
     *
     * @param message сообщение для вывода.
     */
    public void log(@Nullable Object message) {
        if (enabledLogs)
            System.out.println("[Reflectors] " + message);
    }

    /**
     * Преобразовать байты класса в ClassNode.
     *
     * @param bytes байты класса.
     * @return ClassNode из введенных байтов.
     */
    public @NotNull ClassNode readClassFromBytes(byte[] bytes) {
        val classNode = new ClassNode();
        val classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        return classNode;
    }

    /**
     * Прочесть ClassNode класса без его загрузки.
     *
     * @param clazz имя класса.
     * @return ClassNode указанного класса.
     */
    public @NotNull ClassNode readClass(@NotNull String clazz) throws IOException {
        val classNode = new ClassNode();
        val is = Objects.requireNonNull(
                Reflectors.class.getResourceAsStream("/" + clazz.replace('.', '/') + ".class"));
        val classReader = new ClassReader(is);
        is.close();
        classReader.accept(classNode, 0);
        return classNode;
    }

    /**
     * Прочесть ClassNode класса без его загрузки.
     *
     * @param clazz  имя класса.
     * @param loader загрузчик классов, откуда брать рефлектор
     * @return ClassNode указанного класса.
     */
    public @NotNull ClassNode readClass(@NotNull String clazz, @NotNull ClassLoader loader) throws IOException {
        val classNode = new ClassNode();
        val is = Objects.requireNonNull(
                loader.getResourceAsStream("" + clazz.replace('.', '/') + ".class"));
        val classReader = new ClassReader(is);
        is.close();
        classReader.accept(classNode, 0);
        return classNode;
    }

    /**
     * Преобразовать ClassNode в байты.
     *
     * @param classNode ClassNode для преобразования.
     * @param flags     Смотрите {@link ClassWriter#COMPUTE_FRAMES} и {@link ClassWriter#COMPUTE_MAXS}.
     * @return байты класса из ClassNode.
     */
    public byte[] writeClassToBytes(@NotNull ClassNode classNode, int flags) {
        val writer = new ClassWriter(flags);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Найти MethodNode по его имени и дескриптору.
     *
     * @param classNode  класс, где искать метод.
     * @param methodName имя искомого метода.
     * @param methodDesc дескриптор искомого метода.
     * @return найденный MethodNode, либо null в случае неудачи.
     */
    public @Nullable MethodNode findMethodNode(@NotNull ClassNode classNode, @NotNull String methodName,
                                               @NotNull String methodDesc) {
        for (var method : classNode.methods)
            for (var unmappedName : unmapMethodAll(methodName))
                if ((method.name.equals(methodName) || method.name.equals(unmappedName)) &&
                        method.desc.equals(methodDesc))
                    return method;
        return null;
    }

    /**
     * Получить дескриптор метода без первого аргумента.
     *
     * @param method метод для получения дескриптора.
     * @return дескриптор метода без первого аргумента.
     */
    public @NotNull String getMethodDescWithoutFirstArgument(@NotNull MethodNode method) {
        var reflectorParams = Type.getArgumentTypes(method.desc);
        if (reflectorParams.length == 0)
            return method.desc;
        val result = new StringBuilder("(");
        reflectorParams = Arrays.copyOfRange(reflectorParams, 1, reflectorParams.length);
        for (var param : reflectorParams)
            result.append(param.getDescriptor());
        return result.append(")").append(Type.getReturnType(method.desc).getDescriptor()).toString();
    }

    /***
     * Найти целевой метод отражателя, учитывая маппинги.
     *
     * @param classNode класс, где искать метод.
     * @param reflector метод отражатель.
     * @return найденный MethodNode, либо null в случае неудачи.
     */
    public @Nullable MethodNode findMethodNodeByReflector(@NotNull ClassNode classNode, @NotNull MethodNode reflector) {
        val reflectorParams = Type.getArgumentTypes(reflector.desc);
        val flag = reflectorParams.length > 0 && classNode.name.equals(reflectorParams[0].getDescriptor());
        var result = findMethodNode(classNode, reflector.name, reflector.desc);
        if (result != null && (result.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC && !flag)
            return result;
        result = findMethodNode(classNode, reflector.name, getMethodDescWithoutFirstArgument(reflector));
        if (result != null && (result.access & Opcodes.ACC_STATIC) == 0)
            return result;
        return null;
    }

    /**
     * Найти первую инструкцию из тела метода.
     *
     * @param method метод, в котором искать.
     * @return первая инструкция тела метода, либо null в случае неудачи.
     */
    public AbstractInsnNode findFirstInstruction(MethodNode method) {
        for (var instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext())
            if (instruction.getType() != AbstractInsnNode.LABEL && instruction.getType() != AbstractInsnNode.LINE)
                return instruction;
        return null;
    }

    /***
     * Отразить (заменить) целевой метод в целевом классе методом-отражателем.
     * @param classNode целевой класс.
     * @param reflectorClassNode класс отражателя.
     * @param reflectorMethod метод отражатель.
     */
    public void reflectMethod(@NotNull ClassNode classNode, @NotNull ClassNode reflectorClassNode,
                              @NotNull MethodNode reflectorMethod) {
        val method = findMethodNodeByReflector(classNode, reflectorMethod);

        if (method == null) {
            log("Can't find target method!");
            return;
        }

        val replaceData = new InsnList();
        val isStatic = (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;

        loadMethodArguments(replaceData, Type.getArgumentTypes(method.desc), isStatic);

        replaceData.add(new MethodInsnNode(Opcodes.INVOKESTATIC, reflectorClassNode.name, reflectorMethod.name,
                reflectorMethod.desc, false));

        replaceData.add(new InsnNode(getReturnOpcodeFromType(Type.getReturnType(method.desc))));
        method.instructions.insertBefore(findFirstInstruction(method), replaceData);
        log("Method replaced");
    }

    /**
     * Добавить инструкции загрузки для аргументов.
     *
     * @param insnList  список, в который будут добавляться инструкции.
     * @param arguments список аргументов.
     * @param isStatic  для статичных классов "this" переменная не загружается.
     */
    public void loadMethodArguments(@NotNull InsnList insnList, Type[] arguments, boolean isStatic) {
        if (!isStatic) // Add this for non-static method reflections
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (var i = 0; i < arguments.length; i++)
            insnList.add(new VarInsnNode(getLoadOpcodeForType(arguments[i]), i + (isStatic ? 0 : 1)));
    }

    /**
     * Получить опкод загрузки для типа.
     *
     * @param type тип, для которого нужно получить опкод.
     * @return опкод для загрузки данного типа.
     * @see Opcodes
     */
    public int getLoadOpcodeForType(Type type) {
        switch (type.getDescriptor()) {
            case "I":
            case "S":
            case "B":
            case "Z":
            case "C":
                return Opcodes.ILOAD;
            case "J":
                return Opcodes.LLOAD;
            case "D":
                return Opcodes.DLOAD;
            case "F":
                return Opcodes.FLOAD;
            default:
                return Opcodes.ALOAD;
        }
    }

    /**
     * Получить опкод return'а для типа.
     *
     * @param type тип, для которого нужно получить опкод.
     * @return опкод return'а для указанного типа.
     * @see Opcodes
     */
    public int getReturnOpcodeFromType(Type type) {
        switch (type.getDescriptor()) {
            case "I":
            case "S":
            case "B":
            case "C":
            case "Z":
                return Opcodes.IRETURN;
            case "J":
                return Opcodes.LRETURN;
            case "D":
                return Opcodes.DRETURN;
            case "F":
                return Opcodes.FRETURN;
            case "V":
                return Opcodes.RETURN;
            default:
                return Opcodes.ARETURN;
        }
    }

    // ====================
    // Утилиты обфускации
    // ====================

    /**
     * Маппинги методов, где ключом является имя из маппингов, а значением имя из SRG.
     */
    public final BiMap<String, String> METHODS;

    // TODO: Fields getters and setters
//    public final BiMap<String, String> FIELDS;

    /***
     * Загрузить маппинги из файла ресурсов.
     * @param target BiMap, куда будут загружены маппинги.
     * @param resource адрес файла ресурсов, откуда загружать маппинги.
     */
    public void loadMappings(@NotNull BiMap<String, String> target, @NotNull String resource) {
        try {
            var stream = Reflectors.class.getResourceAsStream(resource);
            if (stream == null) {
                log("Can't load mappings from \"" + resource + "\". Resource not found");
                return;
            }
            var reader = new BufferedReader(new InputStreamReader(stream));
            String[] splitted;
            var line = reader.readLine();
            while (line != null) {
                splitted = line.split(",");
                val mapped = splitted[0];
                var unmapped = splitted[1];
                while (METHODS.containsKey(unmapped)) // В данном случае я считаю StringBuilder избыточным,
                    // так как повторок максимум одна-две на имя
                    unmapped += "*";
                target.put(unmapped, mapped);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Преобразовать имя метода из SRG в имя из маппингов.
     * @param name имя метода для преобразования.
     * @return преобразованное имя, либо оно само, если его нет в маппингах.
     */
    public @NotNull String unmapMethod(@NotNull String name) {
        return unmapMethodAll(name).get(0);
    }

    /**
     * Преобразовать имя метода из SRG в имя маппингов.
     *
     * @param name имя метода для преобразования.
     * @return список преобразованных имен, либо оно само, если его нет в маппингах.
     */
    public @NotNull List<String> unmapMethodAll(@NotNull String name) {
        if (!obfuscated)
            return Collections.singletonList(name);
        val result = new ArrayList<String>();
        while (METHODS.containsKey(name)) {
            result.add(METHODS.get(name));
            name += "*";
        }
        if (result.isEmpty())
            result.add(name);
        return result;
    }

    // TODO: Fields getters and setters
//    public @NotNull String unmapField(@NotNull String name) {
//        return obfuscated ? FIELDS.getOrDefault(name, name) : name;
//    }

    /**
     * Преобразовать имя метода из маппингов в SRG
     *
     * @param name имя метода для преобразования.
     * @return преобразованное имя, либо оно само, если его нет в маппингах.
     */
    public @NotNull String mapMethod(@NotNull String name) {
        return obfuscated ? METHODS.inverse().getOrDefault(name, name).replace('*', ' ') : name;
    }

    // TODO: Fields getters and setters
//    public @NotNull String mapField(@NotNull String name) {
//        return obfuscated ? FIELDS.inverse().getOrDefault(name, name) : name;
//    }

    // ====================
    // FMLLoadingPlugin адаптер
    // ====================

    /***
     * Просто адаптер IFMLLoadingPlugin интерфейса для автоматического определения поля
     * {@link Reflectors#obfuscated} и скрытия частых нереализуемых методов. Класс не реализует
     * интерфейс для независимости от классов игры.
     */
    public abstract static class FMLLoadingPluginAdapter {
        public abstract String[] getASMTransformerClass();

        public String getModContainerClass() {
            return null;
        }

        public String getSetupClass() {

            return null;
        }

        public void injectData(Map<String, Object> data) {
            obfuscated = ((Boolean) data.get("runtimeDeobfuscationEnabled"));
            if (obfuscated)
                log("Obfuscated environment");
        }

        public String getAccessTransformerClass() {
            return null;
        }
    }

    static {
        METHODS = HashBiMap.create();
        // TODO: Fields getters and setters
//        FIELDS = HashBiMap.create();

        // From FG mappers
        loadMappings(METHODS, "/methods.csv");
        // TODO: Fields getters and setters
//        loadMappings(FIELDS, "/fields.csv");

        // Just UTF-8 file with "src,mapped" lines
        loadMappings(METHODS, "/methods.txt");
        // TODO: Fields getters and setters
//        loadMappings(FIELDS, "/fields.txt");
    }
}
