<template>
  <div class="editor-page" v-loading="loading">
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
        <h1 class="workflow-title">{{ workflowTitle }}</h1>
        <el-button class="workflow-meta-btn" circle size="small" @click="openMetaDialog" aria-label="工作流信息">
          <svg
            class="workflow-meta-icon"
            width="1em"
            height="1em"
            viewBox="0 0 24 24"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M12 21C16.9705 21 21 16.9705 21 12C21 7.02947 16.9705 2.99997 12 2.99997C7.0295 2.99997 3 7.02947 3 12C3 16.9705 7.0295 21 12 21ZM12 23C5.925 23 1 18.075 1 12C1 5.92497 5.925 0.999969 12 0.999969C18.075 0.999969 23 5.92497 23 12C23 18.075 18.075 23 12 23ZM11 15.5V11.5C10.4477 11.5 10 11.0523 10 10.5C10 9.94768 10.4477 9.49997 11 9.49997H12.0036C12.5545 9.49997 13.0017 9.94549 13.0025 10.4964C13.005 12.1642 13 13.8321 13 15.5H13.5C14.0523 15.5 14.5 15.9477 14.5 16.5C14.5 17.0523 14.0523 17.5 13.5 17.5L10.5 17.5C9.94772 17.5 9.5 17.0523 9.5 16.5C9.5 15.9477 9.94772 15.5 10.5 15.5H11ZM12 8.49997C11.4477 8.49997 11 8.05225 11 7.49997C11 6.94768 11.4477 6.49997 12 6.49997C12.5523 6.49997 13 6.94768 13 7.49997C13 8.05225 12.5523 8.49997 12 8.49997Z"
            ></path>
          </svg>
        </el-button>
      </div>
      <div class="header-right">
        <el-button @click="save" :loading="isSaving">保存</el-button>
        <el-button type="primary" @click="openDebugPanel" :disabled="!isEdit">调试</el-button>
      </div>
    </div>

    <div class="editor-content" :style="contentGridStyle">
      <!-- Left: Palette -->
      <aside v-if="showLeftPanel" class="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-title">节点</div>
          <el-button size="small" @click="showLeftPanel = false">隐藏</el-button>
        </div>
        <div class="sidebar-body">
          <div class="palette">
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('startNodeStart', $event)" @click="addNode('startNodeStart')">
              <div class="palette-badge badge-start">开始</div>
              <div class="palette-desc">定义输入（inputs）</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('knowledgeRetrievalNodeState', $event)" @click="addNode('knowledgeRetrievalNodeState')">
              <div class="palette-badge badge-llm">知识检索</div>
              <div class="palette-desc">inputs → vars.knowledge</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('llmNodeState', $event)" @click="addNode('llmNodeState')">
              <div class="palette-badge badge-llm">大模型</div>
              <div class="palette-desc">Prompt → 输出文本</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('textConcatenationNodeState', $event)" @click="addNode('textConcatenationNodeState')">
              <div class="palette-badge badge-llm">文本串联</div>
              <div class="palette-desc">模板列表 → 输出文本</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('variableAggregationNodeState', $event)" @click="addNode('variableAggregationNodeState')">
              <div class="palette-badge badge-llm">变量聚合</div>
              <div class="palette-desc">变量列表 → 合并结果</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('variableUpdaterNodeState', $event)" @click="addNode('variableUpdaterNodeState')">
              <div class="palette-badge badge-llm">变量更新</div>
              <div class="palette-desc">写入 vars.xxx</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('endNodeEnd', $event)" @click="addNode('endNodeEnd')">
              <div class="palette-badge badge-end">结束</div>
              <div class="palette-desc">定义输出字段</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Middle: Canvas -->
      <section class="canvas-shell">
        <div class="canvas-topbar">
          <div class="topbar-left">
            <el-tag type="info" size="small">手动连线</el-tag>
            <span class="topbar-hint">点击输出端口 → 点击输入端口（点击空白取消）</span>
          </div>
          <div class="topbar-right">
            <el-button size="small" @click="showLeftPanel = !showLeftPanel">{{ showLeftPanel ? '隐藏左侧' : '显示左侧' }}</el-button>
            <el-button size="small" @click="showRightPanel = !showRightPanel">{{ showRightPanel ? '隐藏右侧' : '显示右侧' }}</el-button>
            <el-button size="small" @click="zoomOut" :disabled="view.scale <= VIEW_LIMITS.min">-</el-button>
            <el-button size="small" @click="zoomReset">{{ Math.round(view.scale * 100) }}%</el-button>
            <el-button size="small" @click="zoomIn" :disabled="view.scale >= VIEW_LIMITS.max">+</el-button>
            <el-button size="small" @click="resetView">重置视图</el-button>
            <el-button size="small" @click="fitToScreen" :disabled="nodes.length === 0">适配屏幕</el-button>
            <el-button size="small" @click="ensureTemplate">初始化</el-button>
            <el-button size="small" :disabled="!hasSelection" @click="deleteSelected">删除</el-button>
          </div>
        </div>

        <div
          class="canvas"
          ref="canvasRef"
          @pointerdown="handleCanvasPointerDown"
          @wheel.prevent="handleCanvasWheel"
          @dragover.prevent
          @drop.prevent="onCanvasDrop"
        >
          <div class="canvas-world" :style="worldStyle">
            <div class="canvas-grid" />

            <svg class="canvas-links" :width="canvasSize.width" :height="canvasSize.height">
              <defs>
                <marker
                  id="canvas-edge-arrow"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="8"
                  markerHeight="8"
                  orient="auto"
                  markerUnits="strokeWidth"
                >
                  <path d="M 0 0 L 10 5 L 0 10 z" class="canvas-link-arrow" />
                </marker>
              </defs>
              <path
                v-for="edge in edges"
                :key="edge.from + '->' + edge.to"
                :d="edgePath(edge)"
                :class="['canvas-link', { selected: selectedEdgeKey === edgeKey(edge) }]"
                marker-end="url(#canvas-edge-arrow)"
                @pointerdown.stop.prevent="selectEdge(edge)"
              />
            </svg>

            <div
              v-for="node in nodes"
              :key="node.id"
              class="canvas-node"
              :class="['node-' + node.type, { selected: node.id === selectedNodeId, connecting: node.id === connectingFromId }]"
              :style="{ left: node.x + 'px', top: node.y + 'px' }"
              @pointerdown.stop="selectNode(node.id)"
            >
              <div class="node-header" @pointerdown.stop.prevent="startDrag(node, $event)">
                <div class="node-header-left">
                  <span class="node-dot" />
                  <span class="node-title">{{ nodeTitle(node.type) }}</span>
                </div>
                <span class="node-menu">···</span>
              </div>
              <div class="node-body">
                <div v-if="node.type === 'llmNodeState'" class="node-summary">
                  <div class="node-summary-line">模型：{{ node.model || '未设置' }}</div>
                  <div class="node-summary-line">Prompt：{{ node.prompt || '未设置' }}</div>
                </div>
                <div v-else-if="node.type === 'knowledgeRetrievalNodeState'" class="node-summary">
                  <div class="node-summary-line">Query：{{ node.queryTemplate || '未设置' }}</div>
                  <div class="node-summary-line">输出：{{ node.knowledgeOutputKey || 'knowledge' }}</div>
                </div>
                <div v-else-if="node.type === 'variableUpdaterNodeState'" class="node-summary">
                  <div class="node-summary-line">写入：{{ node.targetKey || '未设置' }}</div>
                  <div class="node-summary-line">值：{{ node.valueTemplate || '未设置' }}</div>
                </div>
                <div v-else-if="node.type === 'textConcatenationNodeState' || node.type === 'variableAggregationNodeState'" class="node-summary">
                  <div class="node-summary-line">行数：{{ (node.partsText || '').split('\n').filter(Boolean).length }}</div>
                  <div class="node-summary-line">输出：{{ node.textOutputKey || (node.type === 'variableAggregationNodeState' ? 'result' : 'text') }}</div>
                </div>
                <div v-else-if="node.type === 'endNodeEnd'" class="node-summary">
                  <div class="node-summary-line">输出：{{ node.outputKey || 'answer' }}</div>
                </div>
                <div v-else class="node-summary">
                  <div class="node-summary-line">输入：inputs</div>
                </div>
              </div>

              <!-- Node-level debug status (last run) -->
              <div v-if="debugHasRun" class="node-debug" @pointerdown.stop>
                <template v-if="traceByNodeId[node.id]">
                  <details class="node-debug-details" :open="node.id === selectedNodeId">
                    <summary class="node-debug-summary" @pointerdown.stop>
                      <div class="node-debug-left">
                        <span
                          :class="[
                            'node-debug-icon',
                            traceByNodeId[node.id].status === 'success'
                              ? 'is-success'
                              : traceByNodeId[node.id].status === 'error'
                                ? 'is-error'
                                : 'is-unknown'
                          ]"
                          aria-hidden="true"
                        >
                          {{ traceByNodeId[node.id].status === 'success' ? '✓' : traceByNodeId[node.id].status === 'error' ? '!' : '·' }}
                        </span>
                        <span class="node-debug-title">
                          {{ traceByNodeId[node.id].status === 'success' ? '运行成功' : traceByNodeId[node.id].status === 'error' ? '运行失败' : '运行状态' }}
                        </span>
                        <span v-if="traceByNodeId[node.id].startedAt != null && traceByNodeId[node.id].finishedAt != null" class="node-debug-time">
                          {{ formatDurationSeconds(traceByNodeId[node.id]) }}
                        </span>
                      </div>
                      <div class="node-debug-right">
                        <span class="node-debug-chevron" aria-hidden="true" />
                      </div>
                    </summary>

                    <div class="node-debug-body">
                      <div v-if="traceByNodeId[node.id].error" class="node-debug-error">
                        {{ traceByNodeId[node.id].error }}
                      </div>

                      <div class="node-debug-label">Output</div>
                      <pre class="node-debug-pre">{{ prettyJson(traceByNodeId[node.id].output) }}</pre>
                    </div>
                  </details>
                </template>
                <template v-else>
                  <div class="node-debug-empty">未执行</div>
                </template>
              </div>

              <div class="node-port port-in" @pointerdown.stop.prevent="completeConnect(node.id)" />
              <div class="node-port port-out" @pointerdown.stop.prevent="beginConnect(node.id)" />
            </div>
          </div>
        </div>
      </section>

      <!-- Right: Inspector -->
      <aside v-if="showRightPanel" class="inspector">
        <div class="inspector-header">
          <div class="inspector-title">{{ inspectorTitle }}</div>
          <div class="inspector-header-actions">
            <el-tag v-if="isEdit" type="info" size="small">ID: {{ workflowId }}</el-tag>
            <button class="panel-close" type="button" @click="showRightPanel = false" aria-label="关闭">
              ×
            </button>
          </div>
        </div>

        <div class="inspector-body">
          <template v-if="inspectorMode === 'node'">
            <el-form v-if="selectedNode" :model="selectedNode" label-position="top" class="node-config">
              <el-form-item label="节点类型">
                <el-input :model-value="nodeTitle(selectedNode.type)" disabled />
              </el-form-item>

              <template v-if="selectedNode.type === 'llmNodeState'">
                <el-form-item label="模型">
                  <el-select v-model="selectedNode.model" placeholder="选择模型" style="width: 100%">
                    <el-option label="qwen-max-latest" value="qwen-max-latest" />
                    <el-option label="qwen-max-2025-01-25" value="qwen-max-2025-01-25" />
                    <el-option label="qwen-max-0919" value="qwen-max-0919" />
                    <el-option label="qwen-max-0428" value="qwen-max-0428" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Prompt">
                  <el-input
                    v-model="selectedNode.prompt"
                    type="textarea"
                    :rows="6"
                    resize="none"
                    placeholder="例如：问题：{{inputs.query}}\n知识：{{vars.knowledge}}"
                  />
                </el-form-item>
                <el-form-item label="输出字段名 (llmOutputKey)">
                  <el-input v-model="selectedNode.llmOutputKey" placeholder="llmOutput" />
                </el-form-item>
                <el-form-item label="绑定插件">
                  <el-select
                    v-model="selectedNode.pluginIds"
                    multiple
                    collapse-tags
                    collapse-tags-indicator
                    placeholder="选择要绑定的插件"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="p in availablePlugins"
                      :key="p.id"
                      :label="p.name"
                      :value="p.id"
                    />
                  </el-select>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'knowledgeRetrievalNodeState'">
                <el-form-item label="Query 模板 (queryTemplate)">
                  <el-input v-model="selectedNode.queryTemplate" placeholder="{{inputs.query}}" />
                </el-form-item>
                <el-form-item label="inputs 中的 agentId 字段名 (agentIdKey)">
                  <el-input v-model="selectedNode.agentIdKey" placeholder="agentId" />
                </el-form-item>
                <el-form-item label="输出字段名 (knowledgeOutputKey)">
                  <el-input v-model="selectedNode.knowledgeOutputKey" placeholder="knowledge" />
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'textConcatenationNodeState' || selectedNode.type === 'variableAggregationNodeState'">
                <el-form-item label="模板列表（每行一个）">
                  <el-input v-model="selectedNode.partsText" type="textarea" :rows="8" resize="none" />
                </el-form-item>
                <el-form-item label="分隔符 (separator)">
                  <el-input v-model="selectedNode.separator" placeholder="\n" />
                </el-form-item>
                <el-form-item label="输出字段名 (textOutputKey)">
                  <el-input v-model="selectedNode.textOutputKey" :placeholder="selectedNode.type === 'variableAggregationNodeState' ? 'result' : 'text'" />
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'variableUpdaterNodeState'">
                <el-form-item label="目标变量名 (targetKey)">
                  <el-input v-model="selectedNode.targetKey" placeholder="answer" />
                </el-form-item>
                <el-form-item label="值模板 (valueTemplate)">
                  <el-input v-model="selectedNode.valueTemplate" placeholder="{{vars.llmOutput}}" />
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'endNodeEnd'">
                <el-form-item label="输出字段名 (outputKey)">
                  <el-input v-model="selectedNode.outputKey" placeholder="answer" />
                </el-form-item>
              </template>

              <template v-else>
                <el-form-item label="说明">
                  <el-input model-value="开始节点用于声明输入参数（inputs）。" disabled />
                </el-form-item>
              </template>
            </el-form>
            <div v-else class="node-config-empty">
              <el-text type="info">点击画布中的节点进行配置</el-text>
            </div>
          </template>

          <template v-else>
            <div class="debug-section">
              <div class="sub-title">输入参数（JSON）</div>
              <el-input
                ref="debugInputsRef"
                v-model="debugInputsText"
                type="textarea"
                :rows="6"
                resize="none"
                placeholder='例如：{"query":"你好"}'
              />

              <div class="debug-actions">
                <el-button type="primary" @click="runDebug" :loading="isRunning">试运行</el-button>
              </div>

              <div class="sub-title">运行结果</div>
              <el-input
                v-model="debugOutputText"
                type="textarea"
                :rows="6"
                resize="none"
                readonly
                placeholder="这里会显示 output（JSON）"
              />

              <div class="sub-title">Trace（按执行顺序，点击可定位节点）</div>
              <div v-if="!debugHasRun" class="trace-empty">
                <el-text type="info">点击“调试运行”后显示</el-text>
              </div>
              <div v-else class="trace-list">
                <div
                  v-for="(evt, idx) in lastTrace"
                  :key="(evt.nodeId || idx) + '-' + idx"
                  class="trace-item"
                  :class="{
                    'is-success': evt.status === 'success',
                    'is-error': evt.status === 'error'
                  }"
                  @click="evt.nodeId && selectNode(evt.nodeId)"
                >
                  <div class="trace-item-title">
                    {{ traceItemTitle(evt) }}
                  </div>
                  <div class="trace-item-meta">
                    {{ evt.status === 'success' ? '成功' : evt.status === 'error' ? '失败' : evt.status }}
                    <span v-if="evt.startedAt != null && evt.finishedAt != null"> · {{ Math.max(0, evt.finishedAt - evt.startedAt) }}ms</span>
                    <span v-if="evt.error"> · {{ evt.error }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </aside>
    </div>

    <el-dialog v-model="metaDialogOpen" title="工作流信息" width="520px">
      <el-form :model="metaDraft" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="metaDraft.name" placeholder="请输入工作流名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="metaDraft.description" type="textarea" :rows="4" placeholder="请输入工作流描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metaDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="applyMeta">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  createWorkflow,
  updateWorkflow,
  getWorkflow,
  debugWorkflow,
  type WorkflowVO
} from '@/api/workflow'
import { listPlugins, type Plugin } from '@/api/plugin'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const isSaving = ref(false)
const isRunning = ref(false)

const inspectorMode = ref<'node' | 'debug'>('node')

const availablePlugins = ref<Plugin[]>([])

async function fetchPlugins() {
  try {
    const res = await listPlugins()
    availablePlugins.value = (res as any) || []
  } catch (error) {
    console.error('获取插件列表失败', error)
  }
}

const workflowTitle = computed(() => {
  const name = (form.value.name || '').trim()
  return name || '未命名工作流'
})

const inspectorTitle = computed(() => {
  return inspectorMode.value === 'debug' ? '调试' : '节点配置'
})

const metaDialogOpen = ref(false)
const metaDraft = ref<{ name: string; description?: string }>({ name: '', description: '' })

function openMetaDialog() {
  metaDraft.value = {
    name: form.value.name,
    description: form.value.description
  }
  metaDialogOpen.value = true
}

async function applyMeta() {
  const name = (metaDraft.value.name || '').trim()
  if (!name) {
    ElMessage.warning('请填写名称')
    return
  }
  form.value.name = name
  form.value.description = metaDraft.value.description

  // Persist immediately for existing workflows so refresh won't lose changes.
  if (workflowId.value) {
    isSaving.value = true
    try {
      const graph = buildGraphString()
      await updateWorkflow(workflowId.value, {
        name: form.value.name,
        description: form.value.description,
        graph
      })
      form.value.graph = graph
      ElMessage.success('已保存')
      metaDialogOpen.value = false
    } catch (error) {
      console.error('更新工作流信息失败', error)
      ElMessage.error('保存失败')
      return
    } finally {
      isSaving.value = false
    }
  } else {
    metaDialogOpen.value = false
    ElMessage.info('请点击“保存”创建工作流后再持久化名称/描述')
  }
}

const showLeftPanel = ref(true)
const showRightPanel = ref(false)

const contentGridStyle = computed(() => {
  const cols: string[] = []
  if (showLeftPanel.value) cols.push('280px')
  cols.push('1fr')
  if (showRightPanel.value) cols.push('360px')
  return {
    gridTemplateColumns: cols.join(' ')
  }
})

const workflowId = computed(() => {
  const raw = route.params.id
  if (!raw) return undefined
  const id = Number(raw)
  return Number.isFinite(id) ? id : undefined
})

const isEdit = computed(() => Boolean(workflowId.value))

const form = ref<{ name: string; description?: string; graph: string }>({
  name: '',
  description: '',
  graph: ''
})

const debugInputsText = ref('')
const debugOutputText = ref('')
const debugInputsRef = ref<any>(null)

interface WorkflowTraceEvent {
  nodeId?: string
  nodeType?: string
  status?: string
  startedAt?: number
  finishedAt?: number
  input?: any
  output?: any
  error?: string
}

const debugHasRun = ref(false)
const lastTrace = ref<WorkflowTraceEvent[]>([])

const traceByNodeId = computed<Record<string, WorkflowTraceEvent>>(() => {
  const map: Record<string, WorkflowTraceEvent> = {}
  for (const evt of lastTrace.value) {
    if (!evt?.nodeId) continue
    map[evt.nodeId] = evt
  }
  return map
})

function prettyJson(value: any): string {
  try {
    return JSON.stringify(value ?? null, null, 2)
  } catch {
    try {
      return String(value)
    } catch {
      return ''
    }
  }
}

function traceItemTitle(evt: WorkflowTraceEvent): string {
  if (evt?.nodeId) {
    const n = nodes.value.find(x => x.id === evt.nodeId)
    if (n) return `${nodeTitle(n.type)} · ${evt.nodeId}`
    return evt.nodeId
  }
  return evt?.nodeType || 'node'
}

function formatDurationSeconds(evt: WorkflowTraceEvent): string {
  const startedAt = evt?.startedAt
  const finishedAt = evt?.finishedAt
  if (startedAt == null || finishedAt == null) return ''
  const ms = Math.max(0, finishedAt - startedAt)
  return (ms / 1000).toFixed(3) + 's'
}

function goBack() {
  router.push('/workflows')
}

function safeParseJson(text: string): any {
  const trimmed = (text || '').trim()
  if (!trimmed) return undefined
  return JSON.parse(trimmed)
}

type NodeType =
  | 'startNodeStart'
  | 'knowledgeRetrievalNodeState'
  | 'textConcatenationNodeState'
  | 'variableAggregationNodeState'
  | 'variableUpdaterNodeState'
  | 'llmNodeState'
  | 'endNodeEnd'

interface CanvasNode {
  id: string
  type: NodeType
  x: number
  y: number
  // llm
  model?: string
  prompt?: string
  llmOutputKey?: string
  pluginIds?: number[]

  // knowledgeRetrieval
  queryTemplate?: string
  agentIdKey?: string
  knowledgeOutputKey?: string

  // textConcatenation / variableAggregation
  partsText?: string
  separator?: string
  textOutputKey?: string

  // variableUpdater
  targetKey?: string
  valueTemplate?: string

  // end
  outputKey?: string
}

interface CanvasEdge {
  from: string
  to: string
}

const NODE_SIZE = { width: 260, height: 96 }

const VIEW_LIMITS = { min: 0.5, max: 2.0 }

const canvasRef = ref<HTMLElement | null>(null)
const canvasSize = reactive({ width: 900, height: 520 })

const view = reactive({ scale: 1, offsetX: 0, offsetY: 0 })
const worldStyle = computed(() => ({
  transform: `translate(${view.offsetX}px, ${view.offsetY}px) scale(${view.scale})`
}))

const nodes = ref<CanvasNode[]>([])
const selectedNodeId = ref<string | null>(null)
const selectedEdgeKey = ref<string | null>(null)

const hasStart = computed(() => nodes.value.some(n => n.type === 'startNodeStart'))
const hasEnd = computed(() => nodes.value.some(n => n.type === 'endNodeEnd'))

const selectedNode = computed(() => nodes.value.find(n => n.id === selectedNodeId.value) || null)
const hasSelection = computed(() => Boolean(selectedNodeId.value || selectedEdgeKey.value))

const edges = ref<CanvasEdge[]>([])
const connectingFromId = ref<string | null>(null)

function edgeKey(edge: CanvasEdge) {
  return `${edge.from}->${edge.to}`
}

function selectEdge(edge: CanvasEdge) {
  selectedNodeId.value = null
  selectedEdgeKey.value = edgeKey(edge)
}

function beginConnect(fromId: string) {
  connectingFromId.value = fromId
  selectedEdgeKey.value = null
}

function completeConnect(toId: string) {
  const fromId = connectingFromId.value
  if (!fromId) return
  connectingFromId.value = null

  if (fromId === toId) return
  if (!nodes.value.some(n => n.id === fromId) || !nodes.value.some(n => n.id === toId)) return
  if (edges.value.some(e => e.from === fromId && e.to === toId)) return
  edges.value = [...edges.value, { from: fromId, to: toId }]
}

function nodeTitle(type: NodeType) {
  if (type === 'startNodeStart') return '开始'
  if (type === 'knowledgeRetrievalNodeState') return '知识检索'
  if (type === 'textConcatenationNodeState') return '文本串联'
  if (type === 'variableAggregationNodeState') return '变量聚合'
  if (type === 'variableUpdaterNodeState') return '变量更新'
  if (type === 'llmNodeState') return '大模型'
  return '结束'
}

function onPaletteDragStart(type: NodeType, e: DragEvent) {
  e.dataTransfer?.setData('application/x-workflow-node-type', type)
}

function onCanvasDrop(e: DragEvent) {
  const type = (e.dataTransfer?.getData('application/x-workflow-node-type') || '') as NodeType
  if (!type) return
  if (type === 'startNodeStart' && hasStart.value) return
  if (type === 'endNodeEnd' && hasEnd.value) return

  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  const x = (e.clientX - rect.left - view.offsetX) / view.scale - NODE_SIZE.width / 2
  const y = (e.clientY - rect.top - view.offsetY) / view.scale - NODE_SIZE.height / 2

  addNode(type, { x, y })
}

function createId(prefix: string) {
  return `${prefix}-${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 8)}`
}

function selectNode(id: string) {
  selectedNodeId.value = id
  selectedEdgeKey.value = null
  // If the inspector is hidden, bring it back on node selection
  showRightPanel.value = true
  inspectorMode.value = 'node'
}

function addNode(type: NodeType, pos?: { x: number; y: number }) {
  if (type === 'startNodeStart' && hasStart.value) return
  if (type === 'endNodeEnd' && hasEnd.value) return

  const baseId =
    type === 'startNodeStart'
      ? 'start'
      : type === 'endNodeEnd'
        ? 'end'
        : type === 'llmNodeState'
          ? 'llm'
          : type.replace(/Node(State|Start|End|Condition)$/g, '').replace(/[^a-zA-Z0-9]+/g, '').toLowerCase() || 'node'
  const safeId = nodes.value.some(n => n.id === baseId) ? createId(baseId) : baseId

  const defaultPos = autoLayoutPositions()
  const base = pos || defaultPos
  const node: CanvasNode = {
    id: safeId,
    type,
    x: base.x,
    y: base.y
  }

  if (type === 'llmNodeState') {
    node.model = 'qwen-max-latest'
    node.prompt = '请结合知识回答：\n\n{{vars.knowledge}}\n\n问题：{{inputs.query}}'
    node.llmOutputKey = 'llmOutput'
  }
  if (type === 'knowledgeRetrievalNodeState') {
    node.queryTemplate = '{{inputs.query}}'
    node.agentIdKey = 'agentId'
    node.knowledgeOutputKey = 'knowledge'
  }
  if (type === 'textConcatenationNodeState') {
    node.partsText = '{{inputs.query}}\n\n{{vars.knowledge}}'
    node.separator = '\n'
    node.textOutputKey = 'text'
  }
  if (type === 'variableAggregationNodeState') {
    node.partsText = '{{vars.knowledge}}\n{{vars.llmOutput}}'
    node.separator = '\n'
    node.textOutputKey = 'result'
  }
  if (type === 'variableUpdaterNodeState') {
    node.targetKey = 'answer'
    node.valueTemplate = '{{vars.llmOutput}}'
  }
  if (type === 'endNodeEnd') {
    node.outputKey = 'answer'
  }

  nodes.value = [...nodes.value, node]
  selectedNodeId.value = node.id
}

function autoLayoutPositions() {
  const y = Math.round(canvasSize.height / 2 - NODE_SIZE.height / 2)
  const startX = 80
  const gap = 120
  const x = startX + nodes.value.length * (NODE_SIZE.width + gap)
  return { x, y }
}

function ensureTemplate() {
  if (nodes.value.length > 0) return
  edges.value = []
  addNode('startNodeStart')
  addNode('knowledgeRetrievalNodeState')
  addNode('llmNodeState')
  addNode('variableUpdaterNodeState')
  addNode('endNodeEnd')
}

function deleteSelected() {
  if (selectedEdgeKey.value) {
    const key = selectedEdgeKey.value
    edges.value = edges.value.filter(e => edgeKey(e) !== key)
    selectedEdgeKey.value = null
    return
  }
  if (!selectedNode.value) return
  const id = selectedNode.value.id
  nodes.value = nodes.value.filter(n => n.id !== id)
  edges.value = edges.value.filter(e => e.from !== id && e.to !== id)
  selectedNodeId.value = null
}

const panState = reactive({
  active: false,
  startClientX: 0,
  startClientY: 0,
  startOffsetX: 0,
  startOffsetY: 0
})

function handleCanvasPointerDown(e: PointerEvent) {
  if (connectingFromId.value) {
    connectingFromId.value = null
    return
  }
  selectedNodeId.value = null
  selectedEdgeKey.value = null
  startPan(e)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key !== 'Delete' && e.key !== 'Backspace') return
  const target = e.target as HTMLElement | null
  if (target) {
    const tag = target.tagName
    const isEditable = (target as any).isContentEditable
    if (isEditable || tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  }
  if (!hasSelection.value) return
  e.preventDefault()
  deleteSelected()
}

function startPan(e: PointerEvent) {
  // Only start pan on background (node pointerdown stops propagation)
  panState.active = true
  panState.startClientX = e.clientX
  panState.startClientY = e.clientY
  panState.startOffsetX = view.offsetX
  panState.startOffsetY = view.offsetY
  window.addEventListener('pointermove', onPanMove)
  window.addEventListener('pointerup', onPanEnd)
}

function onPanMove(e: PointerEvent) {
  if (!panState.active) return
  const dx = e.clientX - panState.startClientX
  const dy = e.clientY - panState.startClientY
  view.offsetX = panState.startOffsetX + dx
  view.offsetY = panState.startOffsetY + dy
}

function onPanEnd() {
  panState.active = false
  window.removeEventListener('pointermove', onPanMove)
  window.removeEventListener('pointerup', onPanEnd)
}

const dragState = reactive({
  active: false,
  nodeId: '' as string,
  startClientX: 0,
  startClientY: 0,
  startX: 0,
  startY: 0
})

function updateCanvasWidth() {
  const el = canvasRef.value
  if (!el) return
  const w = el.clientWidth
  if (w && Number.isFinite(w)) {
    canvasSize.width = w
  }

  const h = el.clientHeight
  if (h && Number.isFinite(h)) {
    canvasSize.height = h
  }
}

function startDrag(node: CanvasNode, e: PointerEvent) {
  selectNode(node.id)
  dragState.active = true
  dragState.nodeId = node.id
  dragState.startClientX = e.clientX
  dragState.startClientY = e.clientY
  dragState.startX = node.x
  dragState.startY = node.y
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragEnd)
}

function onDragMove(e: PointerEvent) {
  if (!dragState.active) return
  const node = nodes.value.find(n => n.id === dragState.nodeId)
  if (!node) return

  const dx = (e.clientX - dragState.startClientX) / view.scale
  const dy = (e.clientY - dragState.startClientY) / view.scale

  const nextX = dragState.startX + dx
  const nextY = dragState.startY + dy

  node.x = nextX
  node.y = nextY
}

function onDragEnd() {
  dragState.active = false
  dragState.nodeId = ''
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
}

function zoomAt(clientX: number, clientY: number, nextScale: number) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) {
    view.scale = nextScale
    return
  }

  const wx = (clientX - rect.left - view.offsetX) / view.scale
  const wy = (clientY - rect.top - view.offsetY) / view.scale

  view.scale = nextScale
  view.offsetX = clientX - rect.left - wx * view.scale
  view.offsetY = clientY - rect.top - wy * view.scale
}

function zoomIn() {
  const rect = canvasRef.value?.getBoundingClientRect()
  const cx = rect ? rect.left + rect.width / 2 : 0
  const cy = rect ? rect.top + rect.height / 2 : 0
  const next = Math.min(VIEW_LIMITS.max, Number((view.scale * 1.1).toFixed(3)))
  zoomAt(cx, cy, next)
}

function zoomOut() {
  const rect = canvasRef.value?.getBoundingClientRect()
  const cx = rect ? rect.left + rect.width / 2 : 0
  const cy = rect ? rect.top + rect.height / 2 : 0
  const next = Math.max(VIEW_LIMITS.min, Number((view.scale / 1.1).toFixed(3)))
  zoomAt(cx, cy, next)
}

function zoomReset() {
  const rect = canvasRef.value?.getBoundingClientRect()
  const cx = rect ? rect.left + rect.width / 2 : 0
  const cy = rect ? rect.top + rect.height / 2 : 0
  zoomAt(cx, cy, 1)
}

function resetView() {
  view.scale = 1
  view.offsetX = 0
  view.offsetY = 0
}

function fitToScreen() {
  if (nodes.value.length === 0) return

  const viewportW = canvasSize.width
  const viewportH = canvasSize.height
  if (!viewportW || !viewportH) return

  const padding = 60

  let minX = Number.POSITIVE_INFINITY
  let minY = Number.POSITIVE_INFINITY
  let maxX = Number.NEGATIVE_INFINITY
  let maxY = Number.NEGATIVE_INFINITY

  for (const n of nodes.value) {
    minX = Math.min(minX, n.x)
    minY = Math.min(minY, n.y)
    maxX = Math.max(maxX, n.x + NODE_SIZE.width)
    maxY = Math.max(maxY, n.y + NODE_SIZE.height)
  }

  const boxW = Math.max(1, maxX - minX)
  const boxH = Math.max(1, maxY - minY)

  const availableW = Math.max(1, viewportW - padding * 2)
  const availableH = Math.max(1, viewportH - padding * 2)

  const rawScale = Math.min(availableW / boxW, availableH / boxH)
  const nextScale = Math.max(VIEW_LIMITS.min, Math.min(VIEW_LIMITS.max, Number(rawScale.toFixed(3))))

  view.scale = nextScale
  // Center the bounding box in viewport
  view.offsetX = (viewportW - boxW * view.scale) / 2 - minX * view.scale
  view.offsetY = (viewportH - boxH * view.scale) / 2 - minY * view.scale
}

function handleCanvasWheel(e: WheelEvent) {
  const direction = e.deltaY > 0 ? -1 : 1
  const factor = direction > 0 ? 1.08 : 1 / 1.08
  const next = Math.max(VIEW_LIMITS.min, Math.min(VIEW_LIMITS.max, Number((view.scale * factor).toFixed(3))))
  if (next === view.scale) return
  zoomAt(e.clientX, e.clientY, next)
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
  window.removeEventListener('pointermove', onPanMove)
  window.removeEventListener('pointerup', onPanEnd)
  window.removeEventListener('resize', updateCanvasWidth)
  window.removeEventListener('keydown', handleKeydown)
})

function edgePoints(edge: CanvasEdge) {
  const from = nodes.value.find(n => n.id === edge.from)
  const to = nodes.value.find(n => n.id === edge.to)
  if (!from || !to) return { x1: 0, y1: 0, x2: 0, y2: 0 }
  return {
    x1: from.x + NODE_SIZE.width,
    y1: from.y + NODE_SIZE.height / 2,
    x2: to.x,
    y2: to.y + NODE_SIZE.height / 2
  }
}

function edgePath(edge: CanvasEdge) {
  const p = edgePoints(edge)
  const dx = Math.max(40, Math.abs(p.x2 - p.x1) * 0.45)
  const c1x = p.x1 + dx
  const c1y = p.y1
  const c2x = p.x2 - dx
  const c2y = p.y2
  return `M ${p.x1} ${p.y1} C ${c1x} ${c1y}, ${c2x} ${c2y}, ${p.x2} ${p.y2}`
}

function buildGraphString() {
  const graph = {
    version: '1.0',
    nodes: nodes.value.map(n => {
      const base: any = {
        id: n.id,
        type: n.type,
        x: n.x,
        y: n.y
      }
      if (n.type === 'llmNodeState') {
        base.model = n.model
        base.prompt = n.prompt
        base.llmOutputKey = n.llmOutputKey || 'llmOutput'
        base.pluginIds = n.pluginIds
      }
      if (n.type === 'knowledgeRetrievalNodeState') {
        base.queryTemplate = n.queryTemplate
        base.agentIdKey = n.agentIdKey || 'agentId'
        base.knowledgeOutputKey = n.knowledgeOutputKey || 'knowledge'
      }
      if (n.type === 'textConcatenationNodeState' || n.type === 'variableAggregationNodeState') {
        base.partsText = n.partsText
        base.separator = n.separator
        base.textOutputKey = n.textOutputKey || (n.type === 'variableAggregationNodeState' ? 'result' : 'text')
      }
      if (n.type === 'variableUpdaterNodeState') {
        base.targetKey = n.targetKey
        base.valueTemplate = n.valueTemplate
      }
      if (n.type === 'endNodeEnd') {
        base.outputKey = n.outputKey || 'answer'
      }
      return base
    }),
    edges: edges.value
  }
  return JSON.stringify(graph, null, 2)
}

function applyGraphString(graphText: string) {
  if (!graphText?.trim()) {
    nodes.value = []
    selectedNodeId.value = null
    return
  }

  try {
    const obj: any = JSON.parse(graphText)
    const rawNodes: any[] = Array.isArray(obj?.nodes) ? obj.nodes : []
    const parsed: CanvasNode[] = rawNodes
      .filter(n => n && typeof n.id === 'string' && typeof n.type === 'string')
      .map(n => {
        // Backward compatibility for legacy graph types
        const rawType = String(n.type)
        const type = (rawType === 'start'
          ? 'startNodeStart'
          : rawType === 'llm'
            ? 'llmNodeState'
            : rawType === 'end'
              ? 'endNodeEnd'
              : rawType) as NodeType
        const x = typeof n.x === 'number' ? n.x : 0
        const y = typeof n.y === 'number' ? n.y : 0
        const node: CanvasNode = { id: n.id, type, x, y }
        if (type === 'llmNodeState') {
          node.model = n.model || 'qwen-max-latest'
          node.prompt = n.prompt || '请结合知识回答：\n\n{{vars.knowledge}}\n\n问题：{{inputs.query}}'
          node.llmOutputKey = n.llmOutputKey || 'llmOutput'
          node.pluginIds = n.pluginIds || []
        }
        if (type === 'knowledgeRetrievalNodeState') {
          node.queryTemplate = n.queryTemplate || '{{inputs.query}}'
          node.agentIdKey = n.agentIdKey || 'agentId'
          node.knowledgeOutputKey = n.knowledgeOutputKey || 'knowledge'
        }
        if (type === 'textConcatenationNodeState' || type === 'variableAggregationNodeState') {
          node.partsText = n.partsText || ''
          node.separator = n.separator ?? '\n'
          node.textOutputKey = n.textOutputKey || (type === 'variableAggregationNodeState' ? 'result' : 'text')
        }
        if (type === 'variableUpdaterNodeState') {
          node.targetKey = n.targetKey || 'answer'
          node.valueTemplate = n.valueTemplate || '{{vars.llmOutput}}'
        }
        if (type === 'endNodeEnd') {
          node.outputKey = n.outputKey || 'answer'
        }
        return node
      })

    // If coordinates are missing, lay them out left-to-right.
    if (parsed.length > 0 && parsed.every(n => n.x === 0 && n.y === 0)) {
      const y = Math.round(canvasSize.height / 2 - NODE_SIZE.height / 2)
      const startX = 80
      const gap = 120
      const ordered = [...parsed]
      for (let i = 0; i < ordered.length; i++) {
        ordered[i].x = startX + i * (NODE_SIZE.width + gap)
        ordered[i].y = y
      }
    }

    const rawEdges: any[] = Array.isArray(obj?.edges) ? obj.edges : []
    const parsedEdges: CanvasEdge[] = rawEdges
      .filter(e => e && typeof e.from === 'string' && typeof e.to === 'string')
      .map(e => ({ from: e.from, to: e.to }))
      .filter(e => e.from !== e.to)

    nodes.value = parsed
    edges.value = parsedEdges
    selectedNodeId.value = null
  } catch (e: any) {
    console.error('解析 graph 失败', e)
    ElMessage.warning('工作流 graph 解析失败，已使用默认模板')
    nodes.value = []
    edges.value = []
    ensureTemplate()
  }
}

async function loadWorkflow() {
  if (!workflowId.value) return
  loading.value = true
  try {
    const data: WorkflowVO = await getWorkflow(workflowId.value)
    form.value.name = data.name
    form.value.description = data.description || ''
    form.value.graph = data.graph || ''
    applyGraphString(form.value.graph)
  } catch (error) {
    console.error('加载工作流失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写名称')
    return
  }

  if (nodes.value.length === 0) {
    ElMessage.warning('请先在画布中添加节点')
    return
  }
  if (!hasStart.value || !hasEnd.value) {
    ElMessage.warning('流程至少需要开始节点和结束节点')
    return
  }

  form.value.graph = buildGraphString()

  isSaving.value = true
  try {
    if (workflowId.value) {
      await updateWorkflow(workflowId.value, {
        name: form.value.name,
        description: form.value.description,
        graph: form.value.graph
      })
      ElMessage.success('保存成功')
    } else {
      const newId = await createWorkflow({
        name: form.value.name,
        description: form.value.description,
        graph: form.value.graph
      })
      ElMessage.success('创建成功')
      router.replace(`/workflows/editor/${newId}`)
    }
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败')
  } finally {
    isSaving.value = false
  }
}

async function runDebug() {
  if (!workflowId.value) {
    ElMessage.warning('请先保存工作流后再调试')
    return
  }

  // Require explicit JSON input; empty means user hasn't provided anything yet.
  const rawText = (debugInputsText.value || '').trim()
  if (!rawText) {
    ElMessage.warning('请先填写调试输入（JSON），例如：{"query":"你好"} 或 {}')
    await nextTick()
    debugInputsRef.value?.focus?.()
    return
  }

  let inputs: any
  try {
    inputs = safeParseJson(debugInputsText.value) || {}
  } catch (e: any) {
    ElMessage.error(`输入参数 JSON 格式错误：${e?.message || '无法解析'}`)
    await nextTick()
    debugInputsRef.value?.focus?.()
    return
  }

  isRunning.value = true
  debugHasRun.value = false
  debugOutputText.value = ''
  lastTrace.value = []
  try {
    const res = await debugWorkflow(workflowId.value, { inputs })
    debugOutputText.value = JSON.stringify(res.output ?? null, null, 2)
    lastTrace.value = Array.isArray(res.trace) ? res.trace : []
    debugHasRun.value = true
    ElMessage.success('运行完成')
  } catch (error) {
    console.error('调试失败', error)
    ElMessage.error('调试失败')
  } finally {
    isRunning.value = false
  }
}

async function openDebugPanel() {
  showRightPanel.value = true
  inspectorMode.value = 'debug'
  await nextTick()
  debugInputsRef.value?.focus?.()
}

onMounted(() => {
  ensureTemplate()
  updateCanvasWidth()
  window.addEventListener('resize', updateCanvasWidth)
  window.addEventListener('keydown', handleKeydown)
  loadWorkflow()
  fetchPlugins()
})
</script>

<style scoped>
.editor-page {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.editor-header {
  height: 64px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-lg);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-left h1 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.workflow-title {
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workflow-meta-btn {
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.workflow-meta-btn:hover {
  color: var(--text-primary);
  border-color: var(--border-color-dark);
}

.workflow-meta-icon {
  display: block;
  font-size: 16px;
}

.editor-content {
  display: grid;
  grid-template-columns: 280px 1fr 360px;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  height: calc(100vh - 64px);
}

.sidebar,
.inspector,
.canvas-shell {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  overflow: hidden;
  min-height: 0;
}

.sidebar {
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.sidebar-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.sidebar-body {
  padding: var(--spacing-md);
  overflow: auto;
}

.palette {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.palette-item {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  background: var(--bg-primary);
  cursor: grab;
  transition: border-color var(--transition-base) ease, box-shadow var(--transition-base) ease;
}

.palette-item:hover {
  border-color: var(--color-primary-lighter);
  box-shadow: var(--shadow-sm);
}

.palette-item:active {
  cursor: grabbing;
}

.palette-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-inverse);
}

.badge-start {
  background: var(--color-info);
}

.badge-llm {
  background: var(--color-primary);
}

.badge-end {
  background: var(--color-success);
}

.palette-desc {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.canvas-shell {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.canvas-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.topbar-hint {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-right {
  display: flex;
  gap: var(--spacing-sm);
}

.section-divider {
  margin: var(--spacing-lg) 0 var(--spacing-md);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-semibold);
}

.section-divider::before {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.section-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.canvas {
  position: relative;
  flex: 1;
  min-height: 0;
  background: var(--bg-secondary);
  overflow: hidden;
  touch-action: none;
}

.canvas-world {
  position: absolute;
  inset: 0;
  transform-origin: 0 0;
  overflow: visible;
}

.canvas-grid {
  position: absolute;
  left: -4000px;
  top: -4000px;
  width: 8000px;
  height: 8000px;
  pointer-events: none;
  background-image:
    radial-gradient(var(--border-color) 1px, transparent 1px);
  background-size: 18px 18px;
}

.canvas-links {
  position: absolute;
  inset: 0;
  pointer-events: auto;
  overflow: visible;
}

.canvas-link {
  fill: none;
  stroke: var(--color-primary-lighter);
  stroke-width: 2;
  pointer-events: stroke;
}

.canvas-link.selected {
  stroke: var(--color-primary);
  stroke-width: 3;
}

.canvas-link-arrow {
  fill: var(--color-primary-lighter);
}

.canvas-node {
  position: absolute;
  width: 260px;
  height: 96px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  user-select: none;
  overflow: visible;
}

.canvas-node.selected {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.canvas-node.connecting {
  border-color: var(--color-primary);
}

.node-header {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-sm);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  cursor: grab;
}

.node-header:active {
  cursor: grabbing;
}

.node-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.node-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
}

.canvas-node.node-start .node-dot {
  background: var(--color-info);
}

.canvas-node.node-end .node-dot {
  background: var(--color-success);
}

.node-title {
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}


.node-menu {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: 1;
}

.node-body {
  padding: var(--spacing-sm);
}

.node-summary-line {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-port {
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  border-radius: var(--radius-full);
  background: var(--bg-primary);
  border: 2px solid var(--border-color-dark);
  transform: translateY(-50%);
}

.port-in {
  left: -6px;
}

.port-out {
  right: -6px;
}

.canvas-node.selected .node-port {
  border-color: var(--color-primary);
}

.inspector {
  display: flex;
  flex-direction: column;
}

.inspector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.inspector-header-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.panel-close {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  line-height: 1;
  font-size: 18px;
  cursor: pointer;
}

.panel-close:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.inspector-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.inspector-body {
  padding: var(--spacing-lg);
  overflow: auto;
}

.node-config-empty {
  padding: var(--spacing-md);
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--bg-tertiary);
}

.right-column {
  padding-bottom: var(--spacing-lg);
}

.debug-section {
  padding: var(--spacing-lg);
}

.debug-actions {
  margin: var(--spacing-md) 0;
  display: flex;
  justify-content: flex-end;
}

.sub-title {
  margin: var(--spacing-md) 0 var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.trace-empty {
  padding: var(--spacing-xs);
}

.trace-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  max-height: 240px;
  overflow: auto;
}

.trace-item {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  cursor: pointer;
}

.trace-item:hover {
  background: var(--bg-secondary);
}

.trace-item-title {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.trace-item-meta {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}

.trace-item.is-error {
  border-color: var(--color-error);
}

.trace-item.is-success {
  border-color: var(--color-success);
}

.node-debug {
  position: absolute;
  left: 0;
  top: calc(100% + 10px);
  width: 100%;
  z-index: 1;
}

.node-debug-details {
  width: 100%;
}

.node-debug-summary {
  list-style: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  cursor: pointer;
  user-select: none;
  padding: 10px 12px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  box-shadow: var(--shadow-sm);
}

.node-debug-summary::-webkit-details-marker {
  display: none;
}

.node-debug-left {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.node-debug-right {
  display: inline-flex;
  align-items: center;
}

.node-debug-icon {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-secondary);
  flex: 0 0 auto;
}

.node-debug-icon.is-success {
  border-color: var(--color-success);
  color: var(--color-success);
  background: var(--bg-secondary);
}

.node-debug-icon.is-error {
  border-color: var(--color-error);
  color: var(--color-error);
  background: var(--bg-secondary);
}

.node-debug-title {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.node-debug-time {
  padding: 2px 8px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
  color: var(--color-success);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.node-debug-chevron {
  width: 10px;
  height: 10px;
  border-right: 2px solid var(--text-secondary);
  border-bottom: 2px solid var(--text-secondary);
  transform: rotate(-45deg);
  opacity: 0.7;
}

details[open] > summary .node-debug-chevron {
  transform: rotate(45deg);
}

.node-debug-body {
  margin-top: var(--spacing-xs);
}

.node-debug-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.node-debug-pre {
  margin: 0;
  padding: var(--spacing-xs);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  max-height: 140px;
  overflow: auto;
  font-size: var(--font-size-xs);
  color: var(--text-primary);
}

.node-debug-error {
  color: var(--color-error);
  font-size: var(--font-size-xs);
  margin-bottom: var(--spacing-xs);
}

.node-debug-empty {
  padding-top: var(--spacing-xs);
  border-top: 1px solid var(--border-color);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
</style>
