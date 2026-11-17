/* Te ʻOhipa : fonctionne hors connexion. Change VERSION à chaque mise à jour publiée. */
const VERSION="te-ohipa-2026.11.16.2";
const CORE=["./","index.html","manifest.webmanifest","icon-192.png","icon-512.png","version.json"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(VERSION).then(c=>c.addAll(CORE)))});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(n=>n!==VERSION).map(n=>caches.delete(n)))).then(()=>self.clients.claim()))});
self.addEventListener("message",e=>{if(e.data==="skip")self.skipWaiting()});
self.addEventListener("fetch",e=>{const r=e.request;if(r.method!=="GET")return;
  e.respondWith(fetch(r).then(res=>{const c=res.clone();caches.open(VERSION).then(x=>x.put(r,c));return res}).catch(()=>caches.match(r).then(m=>m||caches.match("index.html"))))});
