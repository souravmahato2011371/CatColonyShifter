import {Tree} from "@dgreenheck/ez-tree";
const placements=[
 {preset:"Oak Medium",seed:4101,x:-8,z:-5,scale:.092},
 {preset:"Aspen Small",seed:4102,x:-6,z:5,scale:.115},
 {preset:"Pine Small",seed:4103,x:7,z:-6,scale:.112},
 {preset:"Ash Medium",seed:4104,x:9,z:2,scale:.09},
 {preset:"Oak Small",seed:4105,x:5,z:7,scale:.115},
 {preset:"Bush 1",seed:4106,x:-10,z:1,scale:.115},
 {preset:"Bush 2",seed:4107,x:-3,z:8,scale:.11}
];
export function createVerdantTrees(scene){
 const trees=[];
 for(const item of placements){
  const tree=new Tree();
  tree.loadPreset(item.preset);
  tree.options.seed=item.seed;
  tree.options.leaves.count=Math.min(tree.options.leaves.count,10);
  tree.generate();
  tree.position.set(item.x,0,item.z);
  tree.scale.setScalar(item.scale);
  tree.traverse(node=>{if(node.isMesh){node.castShadow=false;node.receiveShadow=false;}});
  scene.add(tree);trees.push(tree);
 }
 let lastWindUpdate=0;
 return{
  update(time){if(time-lastWindUpdate<.09)return;lastWindUpdate=time;for(const tree of trees)tree.update(time);},
  dispose(){for(const tree of trees){tree.traverse(node=>{if(node.isMesh){node.geometry?.dispose();const ms=Array.isArray(node.material)?node.material:[node.material];for(const m of ms)m?.dispose();}});scene.remove(tree);}}
 };
}
