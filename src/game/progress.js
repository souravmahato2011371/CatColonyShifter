export function loadProgress(key,seedCount){
 try{
  const raw=localStorage.getItem(key)||localStorage.getItem("ccs-save")||"{}";
  const s=JSON.parse(raw),foundIds=Array.isArray(s.seedIds)?s.seedIds.filter(i=>Number.isInteger(i)&&i>=0&&i<seedCount):[];
  const built=Array.isArray(s.built)?s.built.filter(x=>x==="garden"||x==="beacon"):[];
  return{foundIds,built,totalFound:Math.max(Number(s.totalFound)||0,foundIds.length,Number(s.collected)||0),currency:Math.max(0,Number(s.currency??s.collected)||0)};
 }catch{return{foundIds:[],built:[],totalFound:0,currency:0};}
}
export function saveProgress(key,s){try{localStorage.setItem(key,JSON.stringify({seedIds:s.foundIds,built:s.built,totalFound:s.totalFound,currency:s.currency}));}catch(e){console.warn("Save unavailable",e);}}
