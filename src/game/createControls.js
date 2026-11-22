export function createControls(canvas){
 const keys=Object.create(null),touch=Object.create(null);let jumpQueued=false,dragging=false,lastX=0,yaw=0;
 function downKey(e){keys[e.key.toLowerCase()]=true;if(e.code==="Space"){e.preventDefault();jumpQueued=true;}if(["arrowup","arrowdown","arrowleft","arrowright"].includes(e.key.toLowerCase()))e.preventDefault();}
 function upKey(e){keys[e.key.toLowerCase()]=false;}
 addEventListener("keydown",downKey);addEventListener("keyup",upKey);
 document.querySelectorAll("[data-move]").forEach(button=>{
  const key=button.dataset.move;
  const down=e=>{e.preventDefault();touch[key]=true;if(key==="jump")jumpQueued=true;};
  const up=e=>{e.preventDefault();touch[key]=false;};
  button.addEventListener("pointerdown",down);button.addEventListener("pointerup",up);button.addEventListener("pointerleave",up);button.addEventListener("pointercancel",up);
 });
 function pointerDown(e){if(e.target.closest("button"))return;dragging=true;lastX=e.clientX;}
 function pointerMove(e){if(dragging){yaw-=(e.clientX-lastX)*.006;lastX=e.clientX;}}
 function pointerUp(){dragging=false;}
 canvas.addEventListener("pointerdown",pointerDown);addEventListener("pointermove",pointerMove);addEventListener("pointerup",pointerUp);addEventListener("pointercancel",pointerUp);
 return {
  getMove(){const f=keys.w||keys.arrowup||touch.forward,b=keys.s||keys.arrowdown||touch.back,l=keys.a||keys.arrowleft||touch.left,r=keys.d||keys.arrowright||touch.right;let x=(r?1:0)-(l?1:0),z=(b?1:0)-(f?1:0),len=Math.hypot(x,z)||1;return{x:x/len,z:z/len,moving:Boolean(x||z)};},
  consumeJump(){const v=jumpQueued;jumpQueued=false;return v;},get yaw(){return yaw;},
  dispose(){removeEventListener("keydown",downKey);removeEventListener("keyup",upKey);canvas.removeEventListener("pointerdown",pointerDown);removeEventListener("pointermove",pointerMove);removeEventListener("pointerup",pointerUp);removeEventListener("pointercancel",pointerUp);}
 };
}
