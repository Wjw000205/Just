<script setup lang="ts">
import {computed,ref} from 'vue'
import {ElMessage} from 'element-plus'

type Task={id:string;name:string;kind:string;dataset:string;algorithm:string;status:string;metric:string;owner:string;time:string;progress:number}

const activeTab=ref('overview'),keyword=ref(''),dialog=ref(false),selected=ref<Task|null>(null)
const tasks=ref<Task[]>([
 {id:'ML-260817-01',name:'增材制造缺陷概率预测',kind:'质量预测',dataset:'SLM 工艺与 CT 检测数据',algorithm:'XGBoost',status:'训练完成',metric:'AUC 0.941',owner:'李明',time:'08-17 09:42',progress:100},
 {id:'ML-260816-07',name:'钛合金疲劳寿命回归',kind:'性能预测',dataset:'TA15 疲劳试验数据',algorithm:'Random Forest',status:'评估中',metric:'R² 0.887',owner:'陈晓',time:'08-16 17:30',progress:82},
 {id:'ML-260816-03',name:'SLM-07 设备异常检测',kind:'异常检测',dataset:'设备时序测点',algorithm:'Isolation Forest',status:'训练中',metric:'—',owner:'王工',time:'08-16 14:08',progress:64},
 {id:'ML-260815-12',name:'涂层结合强度预测',kind:'性能预测',dataset:'等离子喷涂实验数据',algorithm:'LightGBM',status:'待复核',metric:'MAE 2.18',owner:'赵磊',time:'08-15 16:24',progress:100},
 {id:'ML-260814-05',name:'多目标工艺参数推荐',kind:'参数优化',dataset:'工艺实验与仿真结果',algorithm:'Bayesian Opt.',status:'草稿',metric:'—',owner:'李明',time:'08-14 11:16',progress:18}
])
const filteredTasks=computed(()=>{const term=keyword.value.trim().toLowerCase();return term?tasks.value.filter(row=>Object.values(row).some(value=>String(value).toLowerCase().includes(term))):tasks.value})
const features=[
 {name:'激光功率',score:92,tone:'cyan'},{name:'扫描速度',score:81,tone:'blue'},{name:'氧含量',score:74,tone:'violet'},
 {name:'层厚',score:56,tone:'mint'},{name:'粉末复用次数',score:43,tone:'amber'},{name:'基板温度',score:31,tone:'gray'}
]
const points=[18,26,23,39,35,47,51,46,62,58,71,75,69,82,88,85,92,89]
const models=[
 {version:'v4.2',name:'缺陷概率预测',stage:'候选',score:'0.941',metric:'AUC',data:'18,642 条',color:'#41dcf3'},
 {version:'v3.1',name:'疲劳寿命回归',stage:'验证',score:'0.887',metric:'R²',data:'4,208 条',color:'#8e75ff'},
 {version:'v2.6',name:'设备异常检测',stage:'开发',score:'92.4%',metric:'召回率',data:'286 万点',color:'#45d6ac'}
]
function open(row:Task){selected.value=row}
function simulate(action:string){dialog.value=true;ElMessage.info(`${action}为前端演示，训练服务尚未接入`)}
function statusType(status:string){return status==='训练完成'?'success':status==='评估中'||status==='待复核'?'warning':status==='训练中'?'primary':'info'}
</script>

<template>
 <div class="page ml-page">
  <section class="ml-head">
   <div class="head-copy"><p><i></i>MACHINE LEARNING STUDIO</p><div><b>ML</b><h1>机器学习</h1><span><i></i>前端原型</span></div><small>面向质量预测、设备异常检测、性能预测与工艺参数优化的模型研发工作台</small></div>
   <div class="head-actions"><em><i></i>DEMO WORKSPACE</em><el-button @click="simulate('导入模型')">导入模型</el-button><el-button type="primary" @click="simulate('新建训练任务')">＋ 新建训练任务</el-button></div>
  </section>

  <div class="metric-strip">
   <article><span>模型总数</span><strong>18</strong><em>已发布 6</em><i></i></article>
   <article class="mint"><span>最佳模型 AUC</span><strong>0.941</strong><em>较上版 +2.8%</em><i></i></article>
   <article class="violet"><span>运行中任务</span><strong>3</strong><em>GPU 队列 1</em><i></i></article>
   <article class="amber"><span>待复核模型</span><strong>2</strong><em>质量审核待办</em><i></i></article>
  </div>

  <div class="workspace-tabs"><button v-for="tab in [['overview','工作台'],['tasks','训练任务'],['models','模型仓库'],['monitor','模型监控']]" :key="tab[0]" :class="{active:activeTab===tab[0]}" @click="activeTab=tab[0]">{{tab[1]}}</button></div>

  <template v-if="activeTab==='overview'">
   <section class="overview-grid">
    <article class="surface performance-card">
     <header><div><small>MODEL PERFORMANCE</small><h3>缺陷预测 · 验证集表现</h3></div><span>MODEL v4.2</span></header>
     <div class="performance-body">
      <div class="score-ring"><div><strong>94.1</strong><small>AUC SCORE</small></div></div>
      <div class="score-list"><p><span>准确率</span><b>91.8%</b></p><p><span>召回率</span><b>89.6%</b></p><p><span>F1 Score</span><b>90.7%</b></p><p><span>验证样本</span><b>3,728</b></p></div>
     </div>
     <div class="trend"><div class="grid-lines"></div><i v-for="(point,index) in points" :key="index" :style="{height:point+'%'}"></i></div>
     <footer><span>交叉验证 5-Fold</span><span>最后训练 2026-08-17 09:42</span></footer>
    </article>

    <article class="surface feature-card">
     <header><div><small>FEATURE IMPORTANCE</small><h3>关键特征贡献</h3></div><span>SHAP</span></header>
     <div class="feature-list"><div v-for="(feature,index) in features" :key="feature.name"><b>{{String(index+1).padStart(2,'0')}}</b><span>{{feature.name}}</span><i><em :class="feature.tone" :style="{width:feature.score+'%'}"></em></i><strong>{{feature.score}}%</strong></div></div>
     <button @click="simulate('查看完整特征报告')">查看完整特征报告 →</button>
    </article>

    <article class="surface pipeline-card">
     <header><div><small>TRAINING PIPELINE</small><h3>训练流水线</h3></div><span>ACTIVE</span></header>
     <div class="pipeline"><div class="done"><i>✓</i><b>数据快照</b><span>18,642 条</span></div><em></em><div class="done"><i>✓</i><b>特征工程</b><span>32 个特征</span></div><em></em><div class="running"><i></i><b>模型训练</b><span>64%</span></div><em></em><div><i>4</i><b>验证评估</b><span>等待中</span></div><em></em><div><i>5</i><b>人工复核</b><span>未开始</span></div></div>
    </article>

    <article class="surface recent-card">
     <header><div><small>RECENT EXPERIMENTS</small><h3>最近训练任务</h3></div><el-button link @click="activeTab='tasks'">查看全部</el-button></header>
     <el-table :data="tasks.slice(0,4)" @row-click="open"><el-table-column prop="id" label="任务编号" width="135"/><el-table-column prop="name" label="任务名称" min-width="210"/><el-table-column prop="algorithm" label="算法" width="120"/><el-table-column label="状态" width="105"><template #default="{row}"><el-tag :type="statusType(row.status)" effect="dark" size="small">{{row.status}}</el-tag></template></el-table-column><el-table-column prop="metric" label="核心指标" width="110"/><el-table-column prop="time" label="更新时间" width="120"/></el-table>
    </article>
   </section>
  </template>

  <template v-else-if="activeTab==='tasks'">
   <section class="surface table-panel"><header><div><small>TRAINING JOBS</small><h3>训练任务</h3></div><el-input v-model="keyword" clearable placeholder="搜索任务、算法或数据集"/></header><el-table :data="filteredTasks" @row-click="open"><el-table-column prop="id" label="任务编号" width="145"/><el-table-column prop="name" label="任务名称" min-width="220"/><el-table-column prop="kind" label="任务类型" width="110"/><el-table-column prop="dataset" label="训练数据" min-width="200"/><el-table-column prop="algorithm" label="算法" width="130"/><el-table-column label="进度" width="150"><template #default="{row}"><el-progress :percentage="row.progress" :stroke-width="5"/></template></el-table-column><el-table-column label="状态" width="105"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{row.status}}</el-tag></template></el-table-column><el-table-column prop="owner" label="负责人" width="90"/></el-table></section>
  </template>

  <template v-else-if="activeTab==='models'">
   <section class="model-grid"><article v-for="model in models" :key="model.version" class="surface model-card" :style="{'--model-color':model.color}"><header><span>{{model.version}}</span><el-tag effect="plain">{{model.stage}}</el-tag></header><div class="model-icon"><i></i><i></i><i></i><i></i><b>ML</b></div><h3>{{model.name}}</h3><p><strong>{{model.score}}</strong><span>{{model.metric}}</span></p><dl><div><dt>训练数据</dt><dd>{{model.data}}</dd></div><div><dt>算法类型</dt><dd>监督学习</dd></div></dl><footer><el-button @click="simulate('模型对比')">模型对比</el-button><el-button type="primary" @click="simulate('模型详情')">查看详情</el-button></footer></article></section>
  </template>

  <template v-else>
   <section class="monitor-grid"><article class="surface drift-card"><header><div><small>DATA DRIFT</small><h3>输入数据漂移</h3></div><span>近 30 天</span></header><div class="drift-chart"><div v-for="(value,index) in [24,31,28,36,42,38,46,51,48,58,53,61,66,62,69,73,68,76,72,79]" :key="index"><i :style="{height:value+'%'}"></i></div><b></b></div><footer><span><i></i>当前分布</span><span><i></i>训练基线</span><strong>PSI 0.087 · 正常</strong></footer></article><article class="surface alerts-card"><header><div><small>MODEL HEALTH</small><h3>模型健康状态</h3></div><b>3 / 3 正常</b></header><div v-for="model in models" :key="model.version"><i></i><p><b>{{model.name}}</b><span>{{model.version}} · 最近推理 12 分钟前</span></p><strong>健康</strong></div></article></section>
  </template>

  <el-drawer v-model="selected" :title="selected?.name" size="520"><div v-if="selected" class="task-detail"><el-tag :type="statusType(selected.status)" effect="dark">{{selected.status}}</el-tag><h2>{{selected.name}}</h2><p class="mono">{{selected.id}}</p><dl><div><dt>任务类型</dt><dd>{{selected.kind}}</dd></div><div><dt>算法</dt><dd>{{selected.algorithm}}</dd></div><div><dt>训练数据</dt><dd>{{selected.dataset}}</dd></div><div><dt>核心指标</dt><dd>{{selected.metric}}</dd></div><div><dt>负责人</dt><dd>{{selected.owner}}</dd></div><div><dt>更新时间</dt><dd>{{selected.time}}</dd></div></dl><el-progress :percentage="selected.progress"/><el-alert type="info" :closable="false" title="这是演示任务详情；接入计算服务后将展示日志、参数、数据快照和模型文件。"/></div></el-drawer>
  <el-dialog v-model="dialog" title="机器学习原型" width="430"><div class="demo-dialog"><div>ML</div><h3>训练服务尚未接入</h3><p>当前页面用于确认机器学习模块的信息架构和交互方向，不会创建任务或修改平台数据。</p></div><template #footer><el-button type="primary" @click="dialog=false">我知道了</el-button></template></el-dialog>
 </div>
</template>

<style scoped>
.ml-page{color:#c9deed}.ml-head{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:15px}.head-copy>p{font:8px monospace;letter-spacing:.2em;color:#4b8ca9;margin:0 0 9px}.head-copy>p i{display:inline-block;width:22px;height:1px;background:#35d8f6;vertical-align:middle;margin-right:8px;box-shadow:0 0 8px #35d8f6}.head-copy>div{display:flex;align-items:center;gap:11px}.head-copy>div>b{font:800 11px monospace;color:#61e2f7;border:1px solid #24809b;padding:8px;background:#08253a}.head-copy h1{font-size:27px;color:#eefaff;margin:0}.head-copy>div>span{font-size:8px;color:#efb252;border:1px solid rgba(239,178,82,.35);padding:5px 8px}.head-copy>div>span i,.head-actions em i{display:inline-block;width:5px;height:5px;border-radius:50%;background:#efb252;margin-right:6px;box-shadow:0 0 7px #efb252}.head-copy>small{display:block;margin-top:9px;color:#718ca3}.head-actions{display:flex;align-items:center;gap:9px}.head-actions em{font:8px monospace;font-style:normal;color:#5f8197;margin-right:10px}.head-actions em i{background:#51d8b0;box-shadow:0 0 7px #51d8b0}.prototype-alert{margin-bottom:14px}.metric-strip{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:15px}.metric-strip article{min-height:116px;padding:17px 18px;position:relative;overflow:hidden;background:linear-gradient(135deg,#0c2238,#081728);border:1px solid #173b55}.metric-strip article:after{content:"";position:absolute;inset:auto 0 0;height:2px;background:linear-gradient(90deg,#35d8f6,transparent)}.metric-strip span,.metric-strip strong,.metric-strip em{display:block}.metric-strip span{font-size:9px;color:#68859b}.metric-strip strong{font:700 27px monospace;color:#e5fbff;margin:12px 0 7px}.metric-strip em{font-style:normal;font-size:8px;color:#49cfe6}.metric-strip .mint:after{background:linear-gradient(90deg,#43d5ab,transparent)}.metric-strip .violet:after{background:linear-gradient(90deg,#9172ff,transparent)}.metric-strip .amber:after{background:linear-gradient(90deg,#e8a84c,transparent)}.workspace-tabs{display:flex;border-bottom:1px solid #15334a;margin-bottom:14px}.workspace-tabs button{padding:11px 22px;border:0;border-bottom:2px solid transparent;background:none;color:#65849a;font-size:10px;cursor:pointer}.workspace-tabs button.active{color:#5be2f7;border-color:#3ddcf4;background:linear-gradient(transparent,rgba(40,178,213,.08))}.overview-grid{display:grid;grid-template-columns:1.35fr .65fr;gap:13px}.overview-grid article,.table-panel,.monitor-grid article{padding:18px}.overview-grid header,.table-panel>header,.monitor-grid header,.model-card>header{display:flex;align-items:center;justify-content:space-between}.overview-grid header small,.table-panel header small,.monitor-grid header small{font:7px monospace;letter-spacing:.16em;color:#3b91ae}.overview-grid h3,.table-panel h3,.monitor-grid h3{font-size:13px;color:#c8e2ec;margin:5px 0 0}.overview-grid header>span,.monitor-grid header>span{font:7px monospace;color:#58cce1;border:1px solid #22546c;padding:4px 6px}.performance-body{display:grid;grid-template-columns:180px 1fr;align-items:center;margin-top:12px}.score-ring{width:145px;height:145px;border-radius:50%;display:grid;place-items:center;background:radial-gradient(circle,#081b2d 56%,transparent 58%),conic-gradient(#3bdcf2 0 94.1%,#17374b 94.1%);box-shadow:0 0 25px rgba(52,209,236,.1)}.score-ring div{text-align:center}.score-ring strong,.score-ring small{display:block}.score-ring strong{font:700 29px monospace;color:#e0fbff}.score-ring small{font:7px monospace;color:#548399}.score-list{display:grid;grid-template-columns:1fr 1fr;gap:8px}.score-list p{padding:10px;border:1px solid #16364d;background:#071a2c;margin:0}.score-list span,.score-list b{display:block}.score-list span{font-size:7px;color:#55758d}.score-list b{font:700 14px monospace;color:#bde9f1;margin-top:5px}.trend{height:76px;display:flex;align-items:flex-end;gap:4px;border-bottom:1px solid #1b4962;margin-top:8px;position:relative}.trend i{flex:1;min-height:4px;background:linear-gradient(#40ddf2,#15648b);opacity:.82}.performance-card footer,.drift-card footer{display:flex;justify-content:space-between;margin-top:10px;font-size:7px;color:#53748a}.feature-list{margin-top:15px}.feature-list>div{display:grid;grid-template-columns:23px 105px 1fr 34px;align-items:center;gap:8px;margin:13px 0}.feature-list>div>b{font:7px monospace;color:#426b83}.feature-list span{font-size:8px;color:#91adbd}.feature-list>div>i{height:5px;background:#112f43}.feature-list em{display:block;height:100%;background:#3adcf1}.feature-list em.blue{background:#4c82ff}.feature-list em.violet{background:#9070ff}.feature-list em.mint{background:#43d6ad}.feature-list em.amber{background:#e7a647}.feature-list em.gray{background:#557488}.feature-list strong{font:8px monospace;color:#79a4b7}.feature-card>button{width:100%;padding:9px;border:1px solid #1a536d;background:#09243a;color:#4ecce5;font-size:8px}.pipeline-card,.recent-card{grid-column:1/-1}.pipeline{display:flex;align-items:center;justify-content:space-between;margin:24px 8px 7px}.pipeline>div{text-align:center;min-width:105px}.pipeline>div>i{width:28px;height:28px;border-radius:50%;border:1px solid #28556c;display:grid;place-items:center;margin:auto;font-style:normal;font:9px monospace;color:#55768b}.pipeline .done>i{color:#4ad8af;border-color:#3ab790;background:rgba(47,177,141,.1)}.pipeline .running>i{border-color:#46daf0;border-top-color:transparent;animation:spin 1.2s linear infinite}.pipeline b,.pipeline span{display:block}.pipeline b{font-size:8px;color:#9db7c5;margin-top:8px}.pipeline span{font-size:7px;color:#4f7087;margin-top:4px}.pipeline>em{height:1px;flex:1;background:linear-gradient(90deg,#41d4b0,#27687f);margin:0 5px}.recent-card :deep(.el-table){margin-top:12px}.table-panel>header{margin-bottom:13px}.table-panel>header .el-input{width:280px}.model-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:13px}.model-card{padding:18px;border-top-color:var(--model-color)}.model-card>header>span{font:8px monospace;color:var(--model-color)}.model-icon{width:90px;height:90px;margin:24px auto;position:relative;display:grid;place-items:center;border:1px solid color-mix(in srgb,var(--model-color) 45%,transparent);transform:rotate(45deg)}.model-icon i{position:absolute;width:5px;height:5px;background:var(--model-color);box-shadow:0 0 8px var(--model-color)}.model-icon i:nth-child(1){left:-3px;top:-3px}.model-icon i:nth-child(2){right:-3px;top:-3px}.model-icon i:nth-child(3){right:-3px;bottom:-3px}.model-icon i:nth-child(4){left:-3px;bottom:-3px}.model-icon b{font:800 18px monospace;color:var(--model-color);transform:rotate(-45deg)}.model-card h3{text-align:center;color:#cce5ee;font-size:14px}.model-card>p{text-align:center}.model-card>p strong,.model-card>p span{display:block}.model-card>p strong{font:700 26px monospace;color:var(--model-color)}.model-card>p span{font-size:8px;color:#58788d}.model-card dl,.task-detail dl{display:grid;grid-template-columns:1fr 1fr;border:1px solid #17374e}.model-card dl div,.task-detail dl div{padding:10px}.model-card dt,.task-detail dt{font-size:7px;color:#53738a}.model-card dd,.task-detail dd{font-size:9px;color:#a9c6d3;margin:5px 0 0}.model-card footer{display:flex;gap:7px;margin-top:16px}.model-card footer .el-button{flex:1}.monitor-grid{display:grid;grid-template-columns:1.35fr .65fr;gap:13px}.drift-chart{height:310px;display:flex;align-items:flex-end;gap:5px;border-bottom:1px solid #1d4c65;margin-top:14px;background-image:linear-gradient(rgba(43,105,139,.1) 1px,transparent 1px);background-size:100% 62px;position:relative}.drift-chart>div{height:100%;flex:1;display:flex;align-items:flex-end}.drift-chart i{display:block;width:100%;background:linear-gradient(#40d8ee,#174f79)}.drift-chart>b{position:absolute;left:0;right:0;bottom:61%;height:1px;background:#9171ff;box-shadow:0 0 8px #9171ff}.drift-card footer strong{color:#43d5ac}.alerts-card>header>b{font:8px monospace;color:#48d7ae}.alerts-card>div{display:grid;grid-template-columns:8px 1fr 42px;gap:10px;align-items:center;padding:16px 0;border-bottom:1px solid #153148}.alerts-card>div>i{width:7px;height:7px;border-radius:50%;background:#43d8ae;box-shadow:0 0 8px #43d8ae}.alerts-card p{margin:0}.alerts-card p b,.alerts-card p span{display:block}.alerts-card p b{font-size:9px;color:#aac5d2}.alerts-card p span{font-size:7px;color:#54748a;margin-top:5px}.alerts-card>div>strong{font-size:8px;color:#46d4ad}.task-detail h2{color:#dceef5}.task-detail>p{color:#5c839b}.task-detail dl{margin:20px 0}.task-detail .el-alert{margin-top:20px}.demo-dialog{text-align:center;padding:10px 22px}.demo-dialog>div{width:60px;height:60px;border:1px solid #3cd7ef;display:grid;place-items:center;margin:auto;transform:rotate(45deg);color:#5ee6fa;font:800 15px monospace;box-shadow:inset 0 0 18px rgba(60,215,239,.12)}.demo-dialog h3{color:#d1e8f0;margin-top:28px}.demo-dialog p{font-size:10px;line-height:1.7;color:#6e8b9e}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:1100px){.overview-grid,.monitor-grid{grid-template-columns:1fr}.model-grid{grid-template-columns:1fr 1fr}.pipeline{overflow:auto;justify-content:flex-start}.pipeline>div{min-width:110px}.pipeline>em{min-width:50px}}
@media(max-width:760px){.ml-head{align-items:flex-start;flex-direction:column;gap:16px}.head-actions{flex-wrap:wrap}.head-actions em{width:100%}.metric-strip{grid-template-columns:1fr 1fr}.workspace-tabs{overflow:auto}.workspace-tabs button{white-space:nowrap}.performance-body{grid-template-columns:1fr}.score-ring{margin:15px auto}.model-grid{grid-template-columns:1fr}.table-panel{overflow:auto}.table-panel>header{align-items:flex-start;gap:10px;flex-direction:column}.table-panel>header .el-input{width:100%}}
@media(max-width:480px){.metric-strip{grid-template-columns:1fr}.head-copy h1{font-size:22px}.score-list{grid-template-columns:1fr}.pipeline-card{display:none}}
</style>
