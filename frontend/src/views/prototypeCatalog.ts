export type PrototypeSource='DEMO'|'HYBRID'|'PENDING'
export type PrototypeVariant='overview'|'registry'|'lab'|'monitor'|'analysis'|'governance'|'log'
export interface PrototypeMetric{label:string;value:string;unit?:string;trend:string;tone?:'cyan'|'blue'|'mint'|'amber'}
export interface PrototypeColumn{key:string;label:string;wide?:boolean}
export interface PrototypeConfig{code:string;title:string;eyebrow:string;subtitle:string;source:PrototypeSource;variant:PrototypeVariant;metrics:PrototypeMetric[];columns:PrototypeColumn[];rows:Record<string,string>[];series:number[];tabs:string[];insights:string[];actions:string[]}

const registry:PrototypeColumn[]=[{key:'code',label:'业务编码'},{key:'name',label:'名称 / 对象',wide:true},{key:'type',label:'类型'},{key:'status',label:'状态'},{key:'source',label:'来源系统'},{key:'owner',label:'负责人'},{key:'time',label:'更新时间'}]
const analytics:PrototypeColumn[]=[{key:'code',label:'分析编号'},{key:'name',label:'分析对象',wide:true},{key:'type',label:'分析模型'},{key:'status',label:'状态'},{key:'source',label:'数据范围'},{key:'owner',label:'创建人'},{key:'time',label:'更新时间'}]
const metric=(labels:string[]):PrototypeMetric[]=>labels.map((label,index)=>({label,value:['128','86.4','24','1,842'][index],unit:index===1?'%':'',trend:['+12.6%','+4.2%','待处理 3','本月 +208'][index],tone:(['cyan','mint','amber','blue'] as const)[index]}))
const records=(prefix:string,names:string[],types=['标准对象','业务记录','关联数据'])=>names.map((name,index)=>({code:`${prefix}-${String(index+1).padStart(4,'0')}`,name,type:types[index%types.length],status:index===1?'待复核':index===3?'运行中':'有效',source:['平台录入','MES','PLM','ERP','设备网关'][index%5],owner:['李明','王工','陈晓','赵磊'][index%4],time:`2026-08-${String(13-index).padStart(2,'0')} ${String(9+index).padStart(2,'0')}:20`}))
const names={
 material:['TA15医用钛合金','Ti6Al4V ELI','PEEK医用级材料','羟基磷灰石粉体','CoCrMo合金'],
 process:['激光选区熔化工艺 V4','等离子喷涂工艺 V2','表面陶瓷化工艺 V3','五轴精加工路线','真空热处理规范'],
 product:['髋关节股骨柄 HJ-8','多孔髋臼杯 AC-22','脊柱融合器 SC-9','定制颅骨修复体','牙种植体 DT-6'],
 research:['多孔结构骨植入物研发','表面陶瓷化工艺优化','个性化颅骨修复体项目','增材制造粉末复用研究','疲劳寿命预测课题'],
 experiment:['TA15粉末复用实验','多孔结构压缩实验','涂层生物相容性实验','热处理参数窗口实验','表面粗糙度验证'],
 production:['WO-260813 髋臼杯生产','WO-260812 股骨柄精加工','WO-260811 融合器打印','WO-260810 表面喷涂','WO-260809 颅骨修复体'],
 quality:['CT尺寸与缺陷检验','金相组织检验','显微硬度检验','涂层厚度检验','疲劳寿命验证'],
 analysis:['激光功率 × 孔隙率','扫描速度 × 拉伸强度','氧含量 × 缺陷率','喷涂距离 × 结合强度','温度 × 显微硬度'],
 integration:['MES生产执行系统','ERP物料系统','PLM产品生命周期','IoT设备网关','质量检测接口'],
 governance:['材料主数据标准','产品型号主数据','设备主档标准','供应商主数据','组织与产线主数据']
}
type Input={key:string;code:string;title:string;eyebrow:string;subtitle:string;variant:PrototypeVariant;set:keyof typeof names;source?:PrototypeSource;tabs?:string[];labels?:string[];actions?:string[]}
const inputs:Input[]=[
 {key:'assets-overview',code:'02',title:'资产总览',eyebrow:'ENTERPRISE DATA ASSETS',subtitle:'材料、工艺、产品、性能、设备与文件的统一资产门户',variant:'overview',set:'material',source:'HYBRID',tabs:['资产版图','来源分布','质量热力'],labels:['数据资产','完整度','待治理问题','本周新增'],actions:['进入通用数据集','查看文件资料','生成资产报告']},
 {key:'materials',code:'MAT',title:'材料库',eyebrow:'MATERIAL INTELLIGENCE',subtitle:'材料主档、批次、成分、标准、性能与来源映射',variant:'registry',set:'material'},
 {key:'processes',code:'PROC',title:'工艺库',eyebrow:'PROCESS KNOWLEDGE',subtitle:'工艺方案、版本、路线、关键参数与历史应用',variant:'registry',set:'process'},
 {key:'products',code:'PRD',title:'产品库',eyebrow:'PRODUCT DIGITAL THREAD',subtitle:'产品、零件、型号、批次、序列号与全生命周期关联',variant:'registry',set:'product'},
 {key:'performance',code:'PERF',title:'性能数据库',eyebrow:'PERFORMANCE EVIDENCE',subtitle:'力学、硬度、疲劳、生物相容性与涂层性能结果',variant:'analysis',set:'analysis'},
 {key:'trace-history',code:'TRACE',title:'追溯记录',eyebrow:'TRACE SNAPSHOTS',subtitle:'历史追溯、收藏链路、导出快照与查询证据',variant:'log',set:'product',tabs:['最近查询','收藏链路','导出记录']},
 {key:'rnd-projects',code:'RND',title:'研发项目',eyebrow:'R&D PROGRAMS',subtitle:'研发项目、阶段、成员、里程碑与成果的统一视图',variant:'lab',set:'research'},
 {key:'experiments',code:'EXP',title:'实验管理',eyebrow:'EXPERIMENT OPERATIONS',subtitle:'实验计划、样品、执行过程、结果与附件管理',variant:'lab',set:'experiment'},
 {key:'process-experiments',code:'PEX',title:'工艺实验',eyebrow:'PROCESS EXPERIMENTS',subtitle:'材料、设备、参数组合、样品、性能结果与实验结论',variant:'lab',set:'experiment',tabs:['实验矩阵','参数对比','结果判定']},
 {key:'simulations',code:'SIM',title:'仿真管理',eyebrow:'SIMULATION STUDIO',subtitle:'有限元、拓扑优化、热应力任务、模型版本与结果文件',variant:'lab',set:'research',tabs:['任务看板','模型版本','结果云图']},
 {key:'production-overview',code:'05',title:'生产总览',eyebrow:'MANUFACTURING CONTROL',subtitle:'工单、批次、产线、在制品、设备负载与生产异常',variant:'overview',set:'production',tabs:['生产态势','产线负载','异常中心'],labels:['今日工单','计划完成率','生产异常','在制品']},
 {key:'work-orders',code:'WO',title:'工单',eyebrow:'WORK ORDER CENTER',subtitle:'MES工单、计划与实际时间、执行状态、产品及关联批次',variant:'registry',set:'production'},
 {key:'operations',code:'OP',title:'工序',eyebrow:'OPERATION EXECUTION',subtitle:'工艺路线、设备、人员、执行时间、参数与异常结果',variant:'registry',set:'process'},
 {key:'production-batches',code:'BAT',title:'生产批次',eyebrow:'BATCH CONTROL',subtitle:'生产批次与材料、工艺、工单、产品和质量状态关联',variant:'registry',set:'production'},
 {key:'realtime',code:'RT',title:'实时数据',eyebrow:'REAL-TIME TELEMETRY',subtitle:'设备关键参数、阈值、实时趋势与历史回放',variant:'monitor',set:'production',tabs:['实时曲线','历史回放','阈值事件']},
 {key:'inspections',code:'QC',title:'检验记录',eyebrow:'QUALITY INSPECTIONS',subtitle:'检验对象、标准、方法、设备、人员、判定与报告',variant:'registry',set:'quality'},
 {key:'ct-metallography',code:'CT',title:'CT / 金相',eyebrow:'IMAGING QUALITY LAB',subtitle:'CT与金相图像、检测条件、缺陷标记与分析结论',variant:'lab',set:'quality',tabs:['CT检测','金相分析','缺陷标记']},
 {key:'hardness-thickness',code:'HT',title:'硬度 / 厚度',eyebrow:'MEASUREMENT LAB',subtitle:'数值检测、测点分布、上下限、单位和结果判定',variant:'analysis',set:'quality'},
 {key:'fatigue',code:'FT',title:'疲劳试验',eyebrow:'FATIGUE TESTING',subtitle:'载荷、频率、循环次数、S-N结果、设备与试验报告',variant:'lab',set:'quality'},
 {key:'defects',code:'DF',title:'缺陷分析',eyebrow:'DEFECT INTELLIGENCE',subtitle:'缺陷分类、严重度、趋势、批次关联与反向追溯',variant:'analysis',set:'quality'},
 {key:'analysis-process',code:'ANP',title:'工艺对比',eyebrow:'PROCESS BENCHMARK',subtitle:'多工艺方案、参数组合、设备与结果差异对比',variant:'analysis',set:'process'},
 {key:'analysis-performance',code:'ANF',title:'性能对比',eyebrow:'PERFORMANCE BENCHMARK',subtitle:'材料、样品、产品与批次的多性能指标对比',variant:'analysis',set:'analysis'},
 {key:'analysis-batch',code:'ANB',title:'批次对比',eyebrow:'BATCH BENCHMARK',subtitle:'材料、工艺、质量和性能指标的跨批次综合对比',variant:'analysis',set:'production'},
 {key:'correlation',code:'ANC',title:'参数相关性',eyebrow:'CORRELATION LAB',subtitle:'工艺与设备参数对性能、缺陷指标的相关关系分析',variant:'analysis',set:'analysis'},
 {key:'trend-analysis',code:'ANT',title:'趋势分析',eyebrow:'TREND INTELLIGENCE',subtitle:'质量、性能、产量与设备参数的多维时间趋势',variant:'analysis',set:'analysis'},
 {key:'integration-overview',code:'08',title:'集成总览',eyebrow:'INTEGRATION FABRIC',subtitle:'MES、ERP、PLM、IoT连接、同步、吞吐与异常态势',variant:'overview',set:'integration',source:'HYBRID',labels:['接入系统','同步成功率','等待重试','今日数据量'],actions:['进入真实集成配置','查看同步任务','处理异常']},
 {key:'sync-tasks',code:'JOB',title:'同步任务',eyebrow:'SYNC ORCHESTRATION',subtitle:'全量、增量、定时任务、游标、调度周期与重试策略',variant:'monitor',set:'integration',source:'HYBRID'},
 {key:'sync-logs',code:'LOG',title:'同步日志',eyebrow:'SYNC OBSERVABILITY',subtitle:'同步开始结束、数量、耗时、状态与失败详情',variant:'log',set:'integration',source:'HYBRID'},
 {key:'integration-exceptions',code:'DLQ',title:'异常队列',eyebrow:'INTEGRATION EXCEPTIONS',subtitle:'映射缺失、接口超时、格式错误、重试与人工处理',variant:'log',set:'integration',source:'HYBRID'},
 {key:'master-data',code:'MDM',title:'主数据',eyebrow:'MASTER DATA HUB',subtitle:'材料、产品、设备、组织的标准编码、权威来源与映射',variant:'governance',set:'governance'},
 {key:'metadata',code:'META',title:'元数据',eyebrow:'METADATA CATALOG',subtitle:'数据对象、字段、业务定义、来源、责任人与敏感等级',variant:'governance',set:'governance'},
 {key:'units',code:'UNIT',title:'单位管理',eyebrow:'UNIT STANDARDIZATION',subtitle:'计量单位、符号、量纲、基准单位与转换规则',variant:'governance',set:'governance'},
 {key:'data-quality',code:'DQR',title:'数据质量',eyebrow:'DATA QUALITY CONTROL',subtitle:'完整性、唯一性、范围、一致性规则与检查结果',variant:'governance',set:'governance'},
 {key:'system-logs',code:'SYS',title:'系统日志',eyebrow:'SYSTEM OBSERVABILITY',subtitle:'登录、接口、任务、异常与运行日志的统一检索',variant:'log',set:'integration',source:'HYBRID'}
]
export const prototypeCatalog=Object.fromEntries(inputs.map(input=>[input.key,{
 code:input.code,title:input.title,eyebrow:input.eyebrow,subtitle:input.subtitle,variant:input.variant,source:input.source||'DEMO',
 metrics:metric(input.labels||['资产总量','数据完整度','异常 / 待办','近30天新增']),columns:input.variant==='analysis'?analytics:registry,rows:records(input.code,names[input.set]),series:[38,54,48,72,65,86,78,92,88,108,98,122],
 tabs:input.tabs||['综合视图','状态分布','关联关系'],insights:['当前视图遵循角色与数据域边界','关键对象已建立来源系统标识','新增能力为前端演示，不写入数据库'],actions:input.actions||['新建演示','批量导入','导出视图']
}])) as Record<string,PrototypeConfig>
