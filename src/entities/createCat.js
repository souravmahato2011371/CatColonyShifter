import {GLTFLoader} from "three/addons/loaders/GLTFLoader.js";

export function createCat(THREE,scene){
 const cat=new THREE.Group(),fallback=new THREE.Group();
 const fur=new THREE.MeshStandardMaterial({color:"#e9d5ae",roughness:.78}),dark=new THREE.MeshStandardMaterial({color:"#344e52",roughness:.58}),pink=new THREE.MeshStandardMaterial({color:"#df947d",roughness:.72}),eye=new THREE.MeshStandardMaterial({color:"#273c3d",roughness:.25});
 const body=new THREE.Mesh(new THREE.SphereGeometry(.58,18,14),fur);body.scale.set(1,.8,1.15);body.position.y=.75;fallback.add(body);
 const bib=new THREE.Mesh(new THREE.SphereGeometry(.33,14,10),new THREE.MeshStandardMaterial({color:"#fff0d0",roughness:.9}));bib.scale.set(.9,1,.62);bib.position.set(0,.72,-.28);fallback.add(bib);
 const head=new THREE.Group();head.position.set(0,1.36,-.29);fallback.add(head);head.add(new THREE.Mesh(new THREE.SphereGeometry(.48,18,14),fur));
 for(const x of [-.29,.29]){const ear=new THREE.Mesh(new THREE.ConeGeometry(.19,.48,4),fur);ear.position.set(x,.39,0);ear.rotation.z=x<0?.18:-.18;head.add(ear);const inner=new THREE.Mesh(new THREE.ConeGeometry(.09,.28,4),pink);inner.position.set(x,.4,.095);head.add(inner);const eyeMesh=new THREE.Mesh(new THREE.SphereGeometry(.052,10,8),eye);eyeMesh.position.set(x*.68,.04,-.415);head.add(eyeMesh);}
 const nose=new THREE.Mesh(new THREE.SphereGeometry(.045,8,8),pink);nose.position.set(0,-.08,-.47);head.add(nose);
 const tailPivot=new THREE.Group();tailPivot.position.set(0,.88,.48);fallback.add(tailPivot);const tail=new THREE.Mesh(new THREE.TorusGeometry(.35,.095,8,18,Math.PI*1.48),fur);tail.rotation.x=1.1;tailPivot.add(tail);
 const legs=[];for(const x of [-.3,.3])for(const z of [-.28,.29]){const leg=new THREE.Mesh(new THREE.CylinderGeometry(.115,.14,.42,8),fur);leg.position.set(x,.28,z);fallback.add(leg);legs.push({mesh:leg,phase:(x+z)*4});}
 cat.add(fallback);cat.position.set(0,0,5);scene.add(cat);
 let model=null;const parts={};const loader=new GLTFLoader();
 loader.load("./assets/models/cat-player.glb?v=1",gltf=>{model=gltf.scene;model.name="CatColonyPlayerGLB";model.traverse(node=>{if(node.isMesh){node.castShadow=false;node.receiveShadow=false;parts[node.name]=node;}});cat.add(model);fallback.visible=false;},undefined,error=>console.warn("Player GLB unavailable; using procedural fallback.",error));
 function animate(t,moving,grounded){const stride=moving&&grounded?Math.sin(t*13):0;if(model){model.position.y=moving&&grounded?Math.abs(stride)*.035:Math.sin(t*2)*.008;const swing=moving&&grounded?.38:0;for(const [name,phase] of [["FrontLeftLeg",0],["BackRightLeg",0],["FrontRightLeg",Math.PI],["BackLeftLeg",Math.PI]])if(parts[name])parts[name].rotation.x=Math.sin(t*13+phase)*swing;if(parts.Tail)parts.Tail.rotation.z=Math.sin(t*2.4)*.12;}else{body.position.y=.75+(moving&&grounded?Math.abs(stride)*.045:Math.sin(t*2)*.012);head.rotation.z=moving?Math.sin(t*6)*.025:Math.sin(t*1.6)*.018;tailPivot.rotation.x=Math.sin(t*2.4)*.24;tailPivot.rotation.z=Math.sin(t*1.7)*.16;legs.forEach((leg,i)=>leg.mesh.rotation.x=moving&&grounded?Math.sin(t*13+leg.phase)*.42:Math.sin(t*2+i)*.025);}}
 return {group:cat,body,animate};
}