package igentuman.mbtool.util;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;

import java.util.*;


public class AnnotatedInstanceUtil {
    public static ASMDataTable asmData;

    private AnnotatedInstanceUtil() {
    }

    public @interface CapabilityNullHandler {
        String mod() default "";
    }

    public @interface ExtraTileDataProvider {
        String mod() default "";
    }

    public abstract class AbstractNullHandler {
        public AbstractNullHandler() {
        }

        public abstract Capability getCapability();
    }

    public static List<AbstractNullHandler> getNullHandlers() {
        return getInstances(asmData, CapabilityNullHandler.class, AbstractNullHandler.class);
    }

    public static List<AbstractExtraTileDataProvider> getExtraTileDataProviders() {
        return getInstances(asmData, ExtraTileDataProvider.class, AbstractExtraTileDataProvider.class);
    }

    private static <T> List<T> getInstances(ASMDataTable asmDataTable, Class annotationClass, Class<T> instanceClass) {
        String annotationClassName = annotationClass.getCanonicalName();
        Set<ASMData> asmDatas = asmDataTable.getAll(annotationClassName);
        List<T> instances = new ArrayList();
        Iterator var6 = asmDatas.iterator();

        while(var6.hasNext()) {
            ASMData asmData = (ASMData)var6.next();

            try {
                Map<String, Object> annotationInfo = asmData.getAnnotationInfo();
                if (annotationInfo.containsKey("mod")) {
                    String requiredMod = (String)annotationInfo.get("mod");
                    if (requiredMod.length() > 0 && !Loader.isModLoaded(requiredMod)) {
                        continue;
                    }
                }

                Class<?> asmClass = Class.forName(asmData.getClassName());
                Class<? extends T> asmInstanceClass = asmClass.asSubclass(instanceClass);
                T instance = asmInstanceClass.newInstance();
                instances.add(instance);
            } catch (ClassNotFoundException var12) {
            } catch (IllegalAccessException var13) {
            } catch (InstantiationException var14) {
            } catch (ExceptionInInitializerError var15) {
            }
        }

        return instances;
    }

    public static void setAsmData(ASMDataTable asmData) {
        AnnotatedInstanceUtil.asmData = asmData;
    }
}