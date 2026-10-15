/* MeowAlarm 2D Deluxe: screen-only interactions. Android service owns the alarm. */
(() => {
'use strict';
const el = id => document.getElementById(id);
const stage=el('cozyStage'),human=el('cozyHuman'),cat=el('cozyCat');
if(!stage||!human||!cat)return;
const names={pet:'🐾 خودت گربه رو نوازش کن',feed:'🥣 پاکت رو روی کاسه بکش و نگه دار',play:'🪶 پر رو کنار گربه تکون بده',comfort:'🧣 پتو رو روی گربه بنداز',find:'🧶 اسباب‌بازی پنهان رو پیدا کن'};
const routines={lazy:['pet'],breakfast:['pet','feed'],playtime:['play'],grumpy:['pet','find','feed'],custom:['pet','play','comfort','feed']};
const coerce=(x,d)=>Number.isFinite(+x)?+x:d;
const readProfile=()=>{
 try{return JSON.parse(NATIVE&&Android.getCozyProfile?Android.getCozyProfile():(localStorage.getItem('cozy_profile_v7')||'{}'))||{}}catch(e){return{}}
};
let profile={human:'male',cat:'tabby',preset:'breakfast',interactionVolume:.55,onboarded:false,...readProfile()};
if(!['male','female'].includes(profile.human))profile.human='male';
if(!['tabby','orange','black','white','calico'].includes(profile.cat))profile.cat='tabby';
if(!routines[profile.preset])profile.preset='breakfast';
profile.interactionVolume=Math.min(1,Math.max(0,coerce(profile.interactionVolume,.55)));
window.cozyProfile=profile;
const soundSources={purr:'cozy2d/purr_real.ogg',purrFallback:'cozy2d/purr_fallback.wav',bag:'cozy2d/bag.wav',kibble:'cozy2d/kibble.wav',toy:'cozy2d/toy.wav',sigh:'cozy2d/sigh_real.ogg',yawn:'cozy2d/yawn_real.ogg'};
let purrAudio=null,sfx=[];
function playSfx(name){
 const audio=new Audio(soundSources[name]);audio.volume=Math.min(.85,profile.interactionVolume*.9);
 sfx.push(audio);audio.addEventListener('error',()=>{if(name==='sigh'||name==='yawn'){const backup=new Audio('cozy2d/sigh.wav');backup.volume=audio.volume;backup.play().catch(()=>{})}}, {once:true});audio.addEventListener('ended',()=>{sfx=sfx.filter(x=>x!==audio)}, {once:true});
 audio.play().catch(()=>{});
}
function stopPurr(){if(purrAudio){purrAudio.pause();purrAudio.currentTime=0;purrAudio=null}}
function startPurr(){
 stopPurr();purrAudio=new Audio(soundSources.purr);purrAudio.loop=true;purrAudio.volume=profile.interactionVolume*.5;
 purrAudio.addEventListener('error',()=>{if(!purrAudio)return;const fallback=new Audio(soundSources.purrFallback);fallback.loop=true;fallback.volume=profile.interactionVolume*.5;purrAudio=fallback;fallback.play().catch(()=>{})}, {once:true});
 purrAudio.play().catch(()=>{});
}
function persist(){
 const out={human:profile.human,cat:profile.cat,preset:profile.preset,interactionVolume:profile.interactionVolume,onboarded:profile.onboarded};
 try {if(NATIVE&&Android.setCozyProfile)Android.setCozyProfile(JSON.stringify(out));else localStorage.setItem('cozy_profile_v7',JSON.stringify(out))}catch(e){console.warn('Profile save',e)}
 window.cozyProfile=profile;
}
function updateProfile(){
 human.dataset.human=profile.human;cat.dataset.cat=profile.cat;
 document.querySelectorAll('[data-human]').forEach(button=>{
  if(button===human)return;button.classList.toggle('active',button.dataset.human===profile.human);
  button.setAttribute('aria-pressed',String(button.dataset.human===profile.human));
 });
 document.querySelectorAll('[data-cat]').forEach(button=>{
  if(button===cat)return;button.classList.toggle('active',button.dataset.cat===profile.cat);
  button.setAttribute('aria-pressed',String(button.dataset.cat===profile.cat));
 });
 document.querySelectorAll('[data-routine]').forEach(button=>{button.classList.toggle('active',button.dataset.routine===profile.preset);button.setAttribute('aria-pressed',String(button.dataset.routine===profile.preset))});
 el('cozyVolume').value=String(Math.round(profile.interactionVolume*100));el('cozyVolumeText').textContent=Math.round(profile.interactionVolume*100)+'٪';
}
for(const button of document.querySelectorAll('button[data-human]'))button.onclick=()=>{profile.human=button.dataset.human;persist();updateProfile()};
for(const button of document.querySelectorAll('button[data-cat]'))button.onclick=()=>{profile.cat=button.dataset.cat;persist();updateProfile()};
for(const button of document.querySelectorAll('button[data-routine]'))button.onclick=()=>{profile.preset=button.dataset.routine;persist();updateProfile()};
el('cozyVolume').addEventListener('input',e=>{profile.interactionVolume=Number(e.target.value)/100;persist();updateProfile();if(purrAudio)purrAudio.volume=profile.interactionVolume*.5});
el('cozyFinish').onclick=()=>{profile.onboarded=true;persist();el('cozyOnboarding').hidden=true};
updateProfile();
if(!profile.onboarded && !(NATIVE&&Android.isRinging&&Android.isRinging())) el('cozyOnboarding').hidden=false;
function timeEnvironment(){const h=new Date().getHours();const time=h>=5&&h<11?'morning':h>=11&&h<17?'afternoon':h>=17&&h<19?'sunset':'night';stage.dataset.time=time;el('cozyCountdown').textContent=({morning:'☀️ صبح بخیر',afternoon:'🌤️ روز خوش',sunset:'🌅 غروب',night:'🌙 شب بخیر'})[time]}
timeEnvironment();setInterval(timeEnvironment,60*1000);
let story={state:'idle',routine:'breakfast',steps:[],step:0,progress:0,token:0,swipePos:null,drag:null,changedAt:0,holdStart:0,pourAccum:0,snoozeScheduled:false};
function stopVisualSounds(){stopPurr()}
function progress(q){story.progress=Math.max(0,Math.min(1,q));el('cozyProgress').style.width=Math.round(story.progress*100)+'%'}
function moveHuman(position){human.className='cozy-human '+position+' moving';setTimeout(()=>{if(human.classList.contains(position))human.classList.remove('moving')},1250)}
function say(text){el('cozyHelp').textContent=text;el('cozyBubble').textContent=text}
function showStepTools(step){for(const id of ['cozyFoodBag','cozyFeather','cozyBlanket','cozyHiddenToy'])el(id).hidden=true;
 const map={feed:'cozyFoodBag',play:'cozyFeather',comfort:'cozyBlanket',find:'cozyHiddenToy'};if(map[step])el(map[step]).hidden=false;
}
function getSelectedRoutine(){
 // Prefer alarm that Android is currently ringing. Bridge may expose an active alarm id in later builds.
 const nativeId=NATIVE&&Android.getActiveAlarmId?Number(Android.getActiveAlarmId()):null;
 const selected=alarmList.find(x=>x.id===nativeId)||alarmList.find(x=>x.id===focusId)||alarmList.find(x=>x.on);
 return routines[selected?.preset]?selected.preset:profile.preset;
}
function setStep(index){
 if(story.state==='idle'||story.state==='done')return;
 story.step=index;story.swipePos=null;story.drag=null;progress(0);
 const step=story.steps[index];if(!step){finish();return}
 story.state='interacting';stage.dataset.step=step;cat.className='cozy-cat '+(step==='pet'?'happy':'meowing');
 el('cozyActivityTitle').textContent=({pet:'💗 نوازش',feed:'🥣 صبحانهٔ میو',play:'🪶 بازی با پر',comfort:'🧣 خواب راحت',find:'🧶 پیدا کردن اسباب‌بازی'})[step];
 say(names[step]);showStepTools(step);el('cozyComplete').hidden=true;
 if(step==='feed'){moveHuman('cupboard-position');const my=story.token;setTimeout(()=>{if(my===story.token&&story.state==='interacting')moveHuman('bowl-position')},800);playSfx('bag')}
 if(step==='find'){el('cozyHiddenToy').style.left=(42+Math.round(Math.random()*12))+'%';el('cozyHiddenToy').style.top=(49+Math.round(Math.random()*17))+'%'}
}
function startStory(){
 story.token++;stopVisualSounds();story={state:'walking',routine:getSelectedRoutine(),steps:[],step:0,progress:0,token:story.token,swipePos:null,drag:null,changedAt:Date.now(),holdStart:0,pourAccum:0,snoozeScheduled:false};
 story.steps=[...routines[story.routine]];
 for(const id of ['cozyFoodBag','cozyFeather','cozyBlanket']){el(id).style.left='';el(id).style.top=''}
 stage.dataset.step='walking';cat.className='cozy-cat meowing';moveHuman('pet-position');playSfx('sigh');
 el('cozyComplete').hidden=true;el('cozyActivityTitle').textContent='😾 گربه بیدار شد!';say('صدای میو اومد! داره ازت کمک می‌خواد…');showStepTools('');progress(0);
 const my=story.token;setTimeout(()=>{if(my===story.token&&story.state==='walking')setStep(0)},1250);
}
function silenceUntilBackup(){
 if(!NATIVE||story.snoozeScheduled)return;
 try{Android.silence();Android.ringIn(Math.max(60,Number(timings.snoozeSec)||60));story.snoozeScheduled=true}catch(e){console.warn('Snooze fallback failed',e)}
}
function completeStep(){
 if(story.state!=='interacting')return;
 const step=story.steps[story.step];progress(1);
 if(step==='pet'){startPurr();cat.className='cozy-cat sleeping';say('خرخررر… گربه آروم شد! 💛');}
 if(step==='feed'){playSfx('kibble');cat.className='cozy-cat happy';say('غذاش رو خورد! چه روز خوبی 🐾');}
 if(step==='play')playSfx('toy');
 if(story.step<story.steps.length-1){
  if(step==='pet')silenceUntilBackup();
  story.state='transition';stage.dataset.step='transition';showStepTools('');
  const my=story.token;setTimeout(()=>{if(story.token===my&&story.state==='transition')setStep(story.step+1)},900)
 }else finish();
}
function finish(){
 const my=story.token;
 story.state='done';stage.dataset.step='done';showStepTools('');cat.className='cozy-cat happy';stopVisualSounds();
 el('cozyActivityTitle').textContent='✨ آفرین!';say('میو خوشحاله؛ آلارم خاموش شد. روز خوبی داشته باشی!');el('cozyComplete').hidden=false;progress(1);
 if(NATIVE){try{Android.dismiss()}catch(e){console.warn('Dismiss failed',e)}}
 else {try{mode='idle';sleep=true}catch(e){}}
 setTimeout(()=>{if(story.token===my){moveHuman('chilling sitting');playSfx('yawn')}},650);
}
function snoozeNow(){
 if(story.state!=='walking'&&story.state!=='interacting'&&story.state!=='transition')return;
 story.token++;story.state='snoozed';stage.dataset.step='snoozed';cat.className='cozy-cat sleeping';stopVisualSounds();showStepTools('');
 say('میو یه چرت کوچولو می‌زنه 💤');moveHuman('chilling sitting');progress(0);
 if(NATIVE){try{Android.silence();Android.ringIn(Math.max(10,Number(timings.snoozeSec)||60))}catch(e){console.warn(e)}}else{try{mode='idle'}catch(e){}}
}
el('cozySnooze').onclick=snoozeNow;
// Hold-to-skip is intentionally harder than a casual tap, but remains a fallback for accessibility.
const skip=el('cozySkip');skip.textContent='⏭ رد کردن مرحله (نگه دار)';let skipTimer=null;
skip.onpointerdown=()=>{if(story.state!=='interacting')return;skipTimer=setTimeout(()=>{completeStep();skipTimer=null},2000)};
for(const evt of ['pointerup','pointercancel','pointerleave'])skip.addEventListener(evt,()=>{if(skipTimer)clearTimeout(skipTimer);skipTimer=null});
function point(evt){const r=stage.getBoundingClientRect();return{x:(evt.clientX-r.left)/r.width,y:(evt.clientY-r.top)/r.height}}
function intersects(elm,evt){const r=elm.getBoundingClientRect();return evt.clientX>=r.left&&evt.clientX<=r.right&&evt.clientY>=r.top&&evt.clientY<=r.bottom}
function moveDrag(elm,evt){const r=stage.getBoundingClientRect(),w=elm.offsetWidth,h=elm.offsetHeight;elm.style.left=Math.max(0,Math.min(r.width-w,evt.clientX-r.left-w/2))+'px';elm.style.top=Math.max(0,Math.min(r.height-h,evt.clientY-r.top-h/2))+'px'}
let petPointer=null;
cat.addEventListener('pointerdown',evt=>{if(story.state!=='interacting'||story.steps[story.step]!=='pet')return;petPointer=evt.pointerId;story.swipePos=point(evt);cat.setPointerCapture(evt.pointerId);evt.preventDefault()});
cat.addEventListener('pointermove',evt=>{
 if(story.state!=='interacting'||story.steps[story.step]!=='pet'||petPointer!==evt.pointerId||!story.swipePos)return;
 const pt=point(evt),dx=pt.x-story.swipePos.x,dy=pt.y-story.swipePos.y,dist=Math.hypot(dx,dy);
 story.swipePos=pt;if(dist<.008)return;
 progress(story.progress+Math.min(.13,dist*30/(Math.max(1,Number(timings.petSec)||5))));
 cat.classList.add('pet-glow');if(story.progress>=1)completeStep();evt.preventDefault();
});
function endPet(){petPointer=null;story.swipePos=null;cat.classList.remove('pet-glow')}
for(const evt of ['pointerup','pointercancel','lostpointercapture'])cat.addEventListener(evt,endPet);
const movable={cozyFoodBag:'feed',cozyFeather:'play',cozyBlanket:'comfort'};
for(const [id,activity] of Object.entries(movable)){
 const target=el(id);
 target.addEventListener('pointerdown',evt=>{
  if(story.state!=='interacting'||story.steps[story.step]!==activity)return;
  story.drag={id,pointer:evt.pointerId,last:point(evt),lastTime:performance.now(),over:false};target.setPointerCapture(evt.pointerId);evt.preventDefault();
 });
 target.addEventListener('pointermove',evt=>{
  if(!story.drag||story.drag.id!==id||story.drag.pointer!==evt.pointerId)return;
  const pt=point(evt),dist=Math.hypot(pt.x-story.drag.last.x,pt.y-story.drag.last.y);story.drag.last=pt;
  moveDrag(target,evt);
  if(activity==='play'&&intersects(cat,evt)&&dist>.006){progress(story.progress+Math.min(.14,dist*7));if(story.progress>=1)completeStep()}
  if(activity==='feed')story.drag.over=intersects(el('cozyBowlTarget'),evt)||intersects(cat,evt);
  if(activity==='comfort'&&intersects(cat,evt)){progress(story.progress+Math.min(.2,dist*6));if(story.progress>=1)completeStep()}
  evt.preventDefault();
 });
 function drop(evt){if(story.drag&&story.drag.id===id&&story.drag.pointer===evt.pointerId)story.drag=null}
 for(const evt of ['pointerup','pointercancel','lostpointercapture'])target.addEventListener(evt,drop);
}
el('cozyHiddenToy').addEventListener('click',()=>{if(story.state==='interacting'&&story.steps[story.step]==='find')completeStep()});
let previous=performance.now();function animate(now){
 const dt=Math.max(0,Math.min(.06,(now-previous)/1000));previous=now;
 if(story.state==='interacting'&&story.steps[story.step]==='feed'&&story.drag?.id==='cozyFoodBag'&&story.drag.over){
  progress(story.progress+dt/(Math.max(1,Number(timings.feedSec)||4)));
  if(story.progress>=1)completeStep();
 }
 requestAnimationFrame(animate)
}requestAnimationFrame(animate);
// Preserve the native audio service; only wrap the WebView notification callbacks.
const originalNativeRing=window.nativeRing;
window.nativeRing=function(){if(originalNativeRing)originalNativeRing();startStory()};
const originalFire=window.fire;
if(typeof originalFire==='function')window.fire=function(){originalFire();startStory()};
// Do not play the legacy synthesized purr at the same time as the new sound.
try{muted=true;vol()}catch(e){}
el('snd').textContent='🎵 صدای خرخر';
el('snd').onclick=()=>{if(purrAudio)stopPurr();else startPurr()};
const originalStop=el('stopAlarm').onclick;
el('stopAlarm').onclick=function(){story.token++;story.state='idle';stage.dataset.step='idle';stopVisualSounds();showStepTools('');moveHuman('chilling sitting');cat.className='cozy-cat sleeping';progress(0);el('cozyComplete').hidden=true;if(originalStop)originalStop.call(this)};
// When Android launches a ringing alarm before the WebView has finished loading.
if(NATIVE&&Android.isRinging&&Android.isRinging())startStory();
})();
