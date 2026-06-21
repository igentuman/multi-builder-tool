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
        console.log(event.structures[i].getGroup());

        event.structures[i].setStructureNbt(CompoundTag.fromJSON({})); // set structure nbt (you can use this to change the structure, but be careful!)
        var nbt = event.structures[i].getStructureNbt(); // get structure nbt
        nbt.palette[0].Name = 'minecraft:iron_block'; // change first block in palette to iron block
        event.structures[i].setStructureNbt(nbt); // set structure nbt again
        //if you wish to remove structure, then set some non existing block
        nbt.palette[0].Name = 'minecraft:not_existing_block';
        event.structures[i].setStructureNbt(nbt); // set structure nbt again

        // Example if you wish to pack all reactors into Group
        if (event.structures[i].getId().toString().toLowerCase().includes('reactor')) {
            event.structures[i].setGroup('Reactors');
        }
    }
});
```