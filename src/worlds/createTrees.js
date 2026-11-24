import {Tree,TreePreset} from "@dgreenheck/ez-tree";

const placements=[
 {preset:"Oak Medium",seed:4101,x:-8,z:-5,scale:.092},
 {preset:"Aspen Small",seed:4102,x:-6,z:5,scale:.115},
 {preset:"Pine Small",seed:4103,x:7,z:-6,scale:.112},
 {preset:"Ash Medium",seed:4104,x:9,z:2,scale:.09},
 {preset:"Oak Small",seed:4105,x:5,z:7,scale:.115},
 {preset:"Bush 1",seed:4106,x:-10,z:1,scale:.115},
 {preset:"Bush 2",seed:4107,x:-3,z:8,scale:.11},
 // The ancient colony tree uses a full branching oak rather than a sphere canopy.
 {preset:"Oak Large",seed:4108,x:0,z:0,y:.43,scale:.085,leafCount:28,leafTint:0x75b96b}
];

export function createVerdantTrees(scene){
 const trees=[];
 for(const item of placements){
  const preset=TreePreset[item.preset];
  if(!preset)throw new Error("EZ-Tree preset not found: "+item.preset);
  const tree=new Tree();
  tree.options.copy(preset);
  tree.options.seed=item.seed;
  tree.options.leaves.count=item.leafCount??Math.min(tree.options.leaves.count,16);
  if(item.leafTint!==undefined)tree.options.leaves.tint=item.leafTint;
  tree.generate();
  tree.position.set(item.x,item.y??0,item.z);
  tree.scale.setScalar(item.scale);
  tree.traverse(node=>{if(node.isMesh){node.castShadow=false;node.receiveShadow=false;}});
  scene.add(tree);
  trees.push(tree);
 }
 let lastWindUpdate=0;
 return{
  update(time){
   if(time-lastWindUpdate<.09)return;
   lastWindUpdate=time;
   for(const tree of trees)tree.update(time);
  },
  dispose(){
   for(const tree of trees){
    tree.traverse(node=>{
     if(node.isMesh){
      node.geometry?.dispose();
      const materials=Array.isArray(node.material)?node.material:[node.material];
      for(const material of materials)material?.dispose();
     }
    });
    scene.remove(tree);
   }
  }
 };
}
