export function createCat(THREE,scene){
 const cat=new THREE.Group(),fur=new THREE.MeshStandardMaterial({color:"#e9d5ae",roughness:.78}),dark=new THREE.MeshStandardMaterial({color:"#344e52",roughness:.58}),pink=new THREE.MeshStandardMaterial({color:"#df947d",roughness:.72}),eye=new THREE.MeshStandardMaterial({color:"#273c3d",roughness:.25});
 const body=new THREE.Mesh(new THREE.SphereGeometry(.58,18,14),fur);body.scale.set(1,.8,1.15);body.position.y=.75;cat.add(body);
 const bib=new THREE.Mesh(new THREE.SphereGeometry(.33,14,10),new THREE.MeshStandardMaterial({color:"#fff0d0",roughness:.9}));bib.scale.set(.9,1,.62);bib.position.set(0,.72,-.28);cat.add(bib);
 const head=new THREE.Group();head.position.set(0,1.36,-.29);cat.add(head);head.add(new THREE.Mesh(new THREE.SphereGeometry(.48,18,14),fur));
 for(const x of [-.29,.29]){
  const ear=new THREE.Mesh(new THREE.ConeGeometry(.19,.48,4),fur);ear.position.set(x,.39,0);ear.rotation.z=x<0?.18:-.18;head.add(ear);
  const inner=new THREE.Mesh(new THREE.ConeGeometry(.09,.28,4),pink);inner.position.set(x,.4,.095);head.add(inner);
  const eyeMesh=new THREE.Mesh(new THREE.SphereGeometry(.052,10,8),eye);eyeMesh.position.set(x*.68,.04,-.415);head.add(eyeMesh);
  const glint=new THREE.Mesh(new THREE.SphereGeometry(.015,6,6),new THREE.MeshBasicMaterial({color:"#fff"}));glint.position.set(x*.68-.012,.058,-.46);head.add(glint);
 }
 const nose=new THREE.Mesh(new THREE.SphereGeometry(.045,8,8),pink);nose.position.set(0,-.08,-.47);head.add(nose);
 const tailPivot=new THREE.Group();tailPivot.position.set(0,.88,.48);cat.add(tailPivot);
 const tail=new THREE.Mesh(new THREE.TorusGeometry(.35,.095,8,18,Math.PI*1.48),fur);tail.rotation.x=1.1;tailPivot.add(tail);
 const tip=new THREE.Mesh(new THREE.SphereGeometry(.105,8,8),dark);tip.position.set(.32,.18,.03);tailPivot.add(tip);
 const legs=[];
 for(const x of [-.3,.3])for(const z of [-.28,.29]){const leg=new THREE.Mesh(new THREE.CylinderGeometry(.115,.14,.42,8),fur);leg.position.set(x,.28,z);cat.add(leg);legs.push({mesh:leg,phase:(x+z)*4});}
 cat.position.set(0,0,5);cat.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;}});scene.add(cat);
 function animate(t,moving,grounded){const stride=moving&&grounded?Math.sin(t*13):0;body.position.y=.75+(moving&&grounded?Math.abs(stride)*.045:Math.sin(t*2)*.012);head.rotation.z=moving?Math.sin(t*6)*.025:Math.sin(t*1.6)*.018;tailPivot.rotation.x=Math.sin(t*2.4)*.24;tailPivot.rotation.z=Math.sin(t*1.7)*.16;legs.forEach((leg,i)=>{leg.mesh.rotation.x=moving&&grounded?Math.sin(t*13+leg.phase)*.42:Math.sin(t*2+i)*.025;});}
 return {group:cat,body,animate};
}
