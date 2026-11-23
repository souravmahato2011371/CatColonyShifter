export function createInterface(actions){
 const $=id=>document.getElementById(id),panel=$("panel"),map=$("mapPanel"),campButton=$("campBtn"),mapButton=$("mapBtn");let timer=0;
 const setPanel=(node,button,open)=>{node.hidden=!open;node.classList.toggle("open",open);button?.setAttribute("aria-expanded",String(open));};
 const closeAll=()=>{setPanel(panel,campButton,false);setPanel(map,mapButton,false);};
 closeAll();
 $("start").onclick=()=>actions.start();$("continue").onclick=()=>actions.start();
 campButton.onclick=()=>{const open=panel.hidden;closeAll();if(open)setPanel(panel,campButton,true);};
 $("closePanel").onclick=()=>setPanel(panel,campButton,false);
 mapButton.onclick=()=>{const open=map.hidden;closeAll();if(open)setPanel(map,mapButton,true);};
 $("closeMap").onclick=()=>setPanel(map,mapButton,false);
 $("glassBtn").onclick=e=>{const on=actions.toggleGlass();e.currentTarget.textContent=on?"GLASS ON":"GLASS OFF";e.currentTarget.setAttribute("aria-pressed",String(on));};
 document.querySelectorAll("[data-build]").forEach(b=>b.onclick=()=>actions.build(b.dataset.build));
 function toast(message){const node=$("toast");node.textContent=message;node.classList.add("show");clearTimeout(timer);timer=setTimeout(()=>node.classList.remove("show"),1900);}
 function update(s){
  $("count").textContent=s.currency;$("found").textContent=s.totalFound;
  $("colony").textContent=String(1+s.built.length).padStart(2,"0");$("level").textContent=String(1+s.built.length).padStart(2,"0");
  $("bar").style.width=Math.min(100,s.totalFound/s.goal*100)+"%";
  if(s.totalFound>=s.goal){$("questTitle").textContent=s.worldRestored?"A path between worlds":"The gate remembers";$("questText").textContent=s.worldRestored?"Verdant is awake. The wayfinder is ready for the next journey.":"All starseeds found. Return to the ancient gate and step through.";}
  document.querySelectorAll("[data-build]").forEach(b=>{const id=b.dataset.build,cost=id==="garden"?3:5,done=s.built.includes(id);b.textContent=done?"BUILT ✓":"BUILD · "+cost;b.disabled=done;b.setAttribute("aria-label",done?(id+" already built"):( "Build "+id+" for "+cost+" starseeds"));});
 }
 return{toast,update,hideIntro(){$("intro").style.display="none";},hideLoading(){$("loading").classList.add("done");},closePanels:closeAll};
}
