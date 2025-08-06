# Mbtool

This mod adding new tool to build multiblock structures with one click

1. Select what you want to build
2. Put needed items in Mbtool inventory and charge it
3. Click and Build


## KubeJs Support - server script
```javascript
MbtoolKJSEvents.InitMbtoolStructures(event => {
    for(let i = 0; i < event.structures.length; i++) {
        console.log(event.structures[i].getId());
        console.log(event.structures[i].getName());
        console.log(event.structures[i].getStructureNbt());

        event.structures[i].setStructureNbt(CompoundTag.fromJSON({})); // set structure nbt (you can use this to change the structure, but be careful!)
    }
});
```