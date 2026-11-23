export function createControls(canvas){
 const keys=Object.create(null),touch=Object.create(null),buttonCleanups=[];
 let jumpQueued=false,dragging=false,lastX=0,yaw=0,activePointer=null;
 const touchQuery=matchMedia("(pointer: coarse)");
 function setInputMode(mode){document.body.classList.toggle("input-touch",mode==="touch");document.body.classList.toggle("input-keyboard-mouse",mode==="keyboard-mouse");}
 setInputMode(touchQuery.matches?"touch":"keyboard-mouse");
 const onTouchMode=()=>{if(touchQuery.matches)setInputMode("touch");};touchQuery.addEventListener?.("change",onTouchMode);
 function downKey(e){
  if(e.target instanceof HTMLElement&&e.target.closest("input,textarea,select,[contenteditable=true]"))return;
  const key=e.key.toLowerCase();keys[key]=true;setInputMode("keyboard-mouse");
  if(e.code==="Space"){e.preventDefault();jumpQueued=true;}
  if(["arrowup","arrowdown","arrowleft","arrowright"].includes(key))e.preventDefault();
 }
 function upKey(e){keys[e.key.toLowerCase()]=false;}\n function detectPointer(e){setInputMode(e.pointerType==="touch"?"touch":"keyboard-mouse");}\n document.addEventListener("pointerdown",detectPointer,{passive:true});
 addEventListener("keydown",downKey);addEventListener("keyup",upKey);
 document.querySelectorAll("[data-move]").forEach(button=>{
  const key=button.dataset.move;
  const down=e=>{e.preventDefault();touch[key]=true;if(key==="jump")jumpQueued=true;try{button.setPointerCapture(e.pointerId);}catch{}};
  const up=e=>{e.preventDefault();touch[key]=false;};
  button.addEventListener("pointerdown",down);button.addEventListener("pointerup",up);
  button.addEventListener("pointercancel",up);button.addEventListener("lostpointercapture",up);
  buttonCleanups.push(()=>{button.removeEventListener("pointerdown",down);button.removeEventListener("pointerup",up);button.removeEventListener("pointercancel",up);button.removeEventListener("lostpointercapture",up);});
 });
 function pointerDown(e){
  if(e.target.closest("button"))return;
  dragging=true;activePointer=e.pointerId;lastX=e.clientX;
  try{canvas.setPointerCapture(e.pointerId);}catch{}
 }
 function pointerMove(e){if(dragging&&e.pointerId===activePointer){yaw-=(e.clientX-lastX)*.006;lastX=e.clientX;}}
 function pointerUp(e){if(activePointer!==null&&e.pointerId!==activePointer)return;dragging=false;activePointer=null;}
 function contextMenu(e){e.preventDefault();}
 canvas.addEventListener("pointerdown",pointerDown);canvas.addEventListener("pointermove",pointerMove);
 canvas.addEventListener("pointerup",pointerUp);canvas.addEventListener("pointercancel",pointerUp);
 canvas.addEventListener("contextmenu",contextMenu);
 return{
  getMove(){const f=keys.w||keys.arrowup||touch.forward,b=keys.s||keys.arrowdown||touch.back,l=keys.a||keys.arrowleft||touch.left,r=keys.d||keys.arrowright||touch.right;let x=(r?1:0)-(l?1:0),z=(b?1:0)-(f?1:0),len=Math.hypot(x,z)||1;return{x:x/len,z:z/len,moving:Boolean(x||z)};},
  consumeJump(){const v=jumpQueued;jumpQueued=false;return v;},get yaw(){return yaw;},
  dispose(){document.removeEventListener("pointerdown",detectPointer);touchQuery.removeEventListener?.("change",onTouchMode);removeEventListener("keydown",downKey);removeEventListener("keyup",upKey);canvas.removeEventListener("pointerdown",pointerDown);canvas.removeEventListener("pointermove",pointerMove);canvas.removeEventListener("pointerup",pointerUp);canvas.removeEventListener("pointercancel",pointerUp);canvas.removeEventListener("contextmenu",contextMenu);buttonCleanups.forEach(fn=>fn());}
 };
}
