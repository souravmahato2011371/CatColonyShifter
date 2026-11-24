export function loadProgress(key,seedCount){
 try{
  const raw=localStorage.getItem(key)||localStorage.getItem("ccs-save")||"{}";
  const s=JSON.parse(raw);
  const foundIds=Array.isArray(s.seedIds)?[...new Set(s.seedIds.filter(i=>Number.isInteger(i)&&i>=0&&i<seedCount))]:[];
  const built=Array.isArray(s.built)?[...new Set(s.built.filter(x=>x==="garden"||x==="beacon"))]:[];
  const savedFound=Number(s.totalFound)||0,legacyFound=Number(s.collected)||0;
  const totalFound=Math.min(seedCount,Math.max(0,foundIds.length,savedFound,legacyFound));
  const rawCurrency=Number(s.currency??s.collected)||0;
  const currency=Math.max(0,Math.min(999999,Math.floor(rawCurrency)));
  return{foundIds,built,totalFound,currency};
 }catch{return{foundIds:[],built:[],totalFound:0,currency:0};}
}
export function saveProgress(key,s){
 try{
  const seedIds=[...new Set((s.foundIds||[]).filter(Number.isInteger))];
  const built=[...new Set((s.built||[]).filter(x=>x==="garden"||x==="beacon"))];
  localStorage.setItem(key,JSON.stringify({seedIds,built,totalFound:Math.max(seedIds.length,Math.floor(Number(s.totalFound)||0)),currency:Math.max(0,Math.floor(Number(s.currency)||0))}));
 }catch(e){console.warn("Save unavailable",e);}
}
