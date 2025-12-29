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
        <el-button 
          v-if="isEdit && workflowStatus === 'published'"
          @click="handleUnpublish"
          :loading="isPublishing"
        >
          取消发布
        </el-button>
        <el-button 
          v-else-if="isEdit"
          type="success"
          @click="handlePublish"
          :loading="isPublishing"
        >
          发布
        </el-button>
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
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('startNodeStart', $event)">
              <div class="palette-badge badge-start">开始</div>
              <div class="palette-desc">定义输入（inputs）</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('knowledgeRetrievalNodeState', $event)">
              <div class="palette-badge badge-llm">知识检索</div>
              <div class="palette-desc">inputs → vars.knowledge</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('llmNodeState', $event)">
              <div class="palette-badge badge-llm">大模型</div>
              <div class="palette-desc">Prompt → 输出文本</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('textConcatenationNodeState', $event)">
              <div class="palette-badge badge-llm">文本串联</div>
              <div class="palette-desc">模板列表 → 输出文本</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('variableAggregationNodeState', $event)">
              <div class="palette-badge badge-llm">变量聚合</div>
              <div class="palette-desc">变量列表 → 合并结果</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('variableUpdaterNodeState', $event)">
              <div class="palette-badge badge-llm">变量更新</div>
              <div class="palette-desc">写入 vars.xxx</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('textToImageNodeState', $event)">
              <div class="palette-badge badge-llm">文本转图片</div>
              <div class="palette-desc">生成图片</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('intelligentFormNodeState', $event)">
              <div class="palette-badge badge-llm">智能表单</div>
              <div class="palette-desc">智能表单配置</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('conditionNodeCondition', $event)">
              <div class="palette-badge badge-llm">条件</div>
              <div class="palette-desc">条件分支控制</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('httpNodeState', $event)">
              <div class="palette-badge badge-llm">HTTP</div>
              <div class="palette-desc">HTTP请求</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('codeNodeState', $event)">
              <div class="palette-badge badge-llm">代码</div>
              <div class="palette-desc">代码执行</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('loopNodeState', $event)">
              <div class="palette-badge badge-llm">循环</div>
              <div class="palette-desc">循环执行</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('parallelNodeState', $event)">
              <div class="palette-badge badge-llm">并行</div>
              <div class="palette-desc">并行执行</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('manualCheckNodeState', $event)">
              <div class="palette-badge badge-llm">人工检查</div>
              <div class="palette-desc">人工检查配置</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('replyNodeState', $event)">
              <div class="palette-badge badge-llm">回复</div>
              <div class="palette-desc">回复消息</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('toolInvokeNodeState', $event)">
              <div class="palette-badge badge-llm">工具调用</div>
              <div class="palette-desc">调用外部工具</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('fileExtractionNodeState', $event)">
              <div class="palette-badge badge-llm">文件提取</div>
              <div class="palette-desc">提取文件内容</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('questionClassificationNodeState', $event)">
              <div class="palette-badge badge-llm">问题分类</div>
              <div class="palette-desc">问题分类</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('queryOptimizationNodeState', $event)">
              <div class="palette-badge badge-llm">查询优化</div>
              <div class="palette-desc">优化查询</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('textExtractionNodeState', $event)">
              <div class="palette-badge badge-llm">文本提取</div>
              <div class="palette-desc">提取文本信息</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('evaluationAlgorithmsNodeState', $event)">
              <div class="palette-badge badge-llm">评估算法</div>
              <div class="palette-desc">评估算法</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('evaluationTestSetNodeState', $event)">
              <div class="palette-badge badge-llm">评估测试集</div>
              <div class="palette-desc">评估测试集</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('evaluationStartNodeState', $event)">
              <div class="palette-badge badge-llm">评估开始</div>
              <div class="palette-desc">开始评估</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('evaluationEndNodeState', $event)">
              <div class="palette-badge badge-llm">评估结束</div>
              <div class="palette-desc">结束评估</div>
            </div>

            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('extractorNodeState', $event)">
              <div class="palette-badge badge-llm">提取</div>
              <div class="palette-desc">数据提取</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="onPaletteDragStart('endNodeEnd', $event)">
              <div class="palette-badge badge-end">结束</div>
              <div class="palette-desc">定义输出字段</div>
            </div>
          </div>
        </div>
        <!-- Left Resizer -->
        <div 
          class="resizer resizer-left"
          @pointerdown="startResize('left', $event)"
        />
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
          @pointermove="handleCanvasPointerMove"
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
              <!-- 临时连接线 -->
              <path
                v-if="connectingFromId"
                :d="tempEdgePath"
                class="canvas-link temp"
                marker-end="url(#canvas-edge-arrow)"
                stroke-dasharray="5,5"
              />
            </svg>

            <div
              v-for="node in nodes"
              :key="node.id"
              :data-node-id="node.id"
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
                  <div class="node-summary-line">智能体ID：{{ node.llmAgentId || '未设置' }}</div>
                  <div class="node-summary-line">Prompt：{{ node.prompt || '未设置' }}</div>
                </div>
                <div v-else-if="node.type === 'knowledgeRetrievalNodeState'" class="node-summary">
                  <div class="node-summary-line">Query：{{ node.queryTemplate || '未设置' }}</div>
                  <div class="node-summary-line" v-if="node.knowledgeBaseId">
                    知识库：{{ availableKnowledgeBases.find(kb => kb.id === node.knowledgeBaseId)?.name || `ID: ${node.knowledgeBaseId}` }}
                  </div>
                  <div class="node-summary-line" v-else-if="node.agentId">
                    Agent：{{ availableAgents.find(a => a.id === node.agentId)?.name || `ID: ${node.agentId}` }}
                  </div>
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
                <div v-else-if="node.type === 'conditionNodeCondition'" class="node-summary">
                  <div class="node-summary-line">分支数：{{ node.conditionParams?.branches?.length || 0 }}</div>
                  <div class="node-summary-line">条件：{{ node.conditionParams?.branches?.[0]?.conditions?.[0]?.condition || '未设置' }}</div>
                </div>
                <div v-else-if="node.type === 'httpNodeState'" class="node-summary">
                  <div class="node-summary-line">方法：{{ node.httpMethod || 'GET' }}</div>
                  <div class="node-summary-line">URL：{{ node.httpUrl || '未设置' }}</div>
                </div>
                <div v-else-if="node.type === 'codeNodeState'" class="node-summary">
                  <div class="node-summary-line">语言：{{ node.codeLanguage || 'javascript' }}</div>
                  <div class="node-summary-line">输出：{{ node.codeOutputKey || 'codeResult' }}</div>
                </div>
                <div v-else-if="node.type === 'loopNodeState'" class="node-summary">
                  <div class="node-summary-line">变量：{{ node.loopIterationVar || 'item' }}</div>
                  <div class="node-summary-line">输出：{{ node.loopOutputKey || 'loopResults' }}</div>
                  <div class="node-summary-line">说明：连接两个节点形成循环路径</div>
                </div>
                <div v-else-if="node.type === 'parallelNodeState'" class="node-summary">
                  <div class="node-summary-line">任务数：{{ parallelTaskCount(node) }}</div>
                  <div class="node-summary-line">输出：{{ node.parallelOutputKey || 'parallelResults' }}</div>
                </div>
                <div v-else-if="node.type === 'manualCheckNodeState'" class="node-summary">
                  <div class="node-summary-line">表单ID：{{ node.manualCheckFormId || '未设置' }}</div>
                  <div class="node-summary-line">描述：{{ node.manualCheckStageDesc || '请检查以下内容' }}</div>
                </div>
                <div v-else-if="node.type === 'replyNodeState'" class="node-summary">
                  <div class="node-summary-line">类型：{{ node.replyMessageType || 'text' }}</div>
                  <div class="node-summary-line">流式：{{ node.replyEnableStreaming ? '是' : '否' }}</div>
                </div>
                <div v-else-if="node.type === 'toolInvokeNodeState'" class="node-summary">
                  <div class="node-summary-line">工具ID：{{ node.toolId || '未设置' }}</div>
                  <div class="node-summary-line">输出：{{ node.toolOutputVar || 'toolResult' }}</div>
                </div>
                <div v-else-if="node.type === 'textToImageNodeState'" class="node-summary">
                  <div class="node-summary-line">智能体ID：{{ node.textToImageAgentId || '未设置' }}</div>
                  <div class="node-summary-line">输出：{{ node.textToImageOutputKey || 'imageUrl' }}</div>
                </div>
                <div v-else-if="node.type === 'fileExtractionNodeState'" class="node-summary">
                  <div class="node-summary-line">类型：{{ node.fileExtractionType || 'text' }}</div>
                  <div class="node-summary-line">输出：{{ node.fileExtractionOutputKey || 'fileContent' }}</div>
                </div>
                <div v-else-if="node.type === 'questionClassificationNodeState'" class="node-summary">
                  <div class="node-summary-line">智能体ID：{{ node.questionClassificationAgentId || '未设置' }}</div>
                  <div class="node-summary-line">输出：{{ node.questionClassificationOutputKey || 'classification' }}</div>
                </div>
                <div v-else-if="node.type === 'queryOptimizationNodeState'" class="node-summary">
                  <div class="node-summary-line">智能体ID：{{ node.queryOptimizationAgentId || '未设置' }}</div>
                  <div class="node-summary-line">输出：{{ node.queryOptimizationOutputKey || 'optimizedQuery' }}</div>
                </div>
                <div v-else-if="node.type === 'textExtractionNodeState'" class="node-summary">
                  <div class="node-summary-line">类型：{{ node.textExtractionType || 'keyword' }}</div>
                  <div class="node-summary-line">输出：{{ node.textExtractionOutputKey || 'extractionResult' }}</div>
                </div>
                <div v-else-if="node.type === 'evaluationAlgorithmsNodeState'" class="node-summary">
                  <div class="node-summary-line">算法：{{ node.evaluationAlgorithmsAlgorithm || 'accuracy' }}</div>
                  <div class="node-summary-line">输出：{{ node.evaluationAlgorithmsOutputKey || 'evaluationResult' }}</div>
                </div>
                <div v-else-if="node.type === 'evaluationTestSetNodeState'" class="node-summary">
                  <div class="node-summary-line">测试集：{{ node.evaluationTestSetTestSet || 'default' }}</div>
                  <div class="node-summary-line">输出：{{ node.evaluationTestSetOutputKey || 'testSetResult' }}</div>
                </div>
                <div v-else-if="node.type === 'evaluationStartNodeState'" class="node-summary">
                  <div class="node-summary-line">触发模式：{{ node.evaluationStartTriggerMode || 'auto' }}</div>
                </div>
                <div v-else-if="node.type === 'evaluationEndNodeState'" class="node-summary">
                  <div class="node-summary-line">输出：{{ node.evaluationEndOutputKey || 'evaluationResult' }}</div>
                </div>
                <div v-else-if="node.type === 'intelligentFormNodeState'" class="node-summary">
                  <div class="node-summary-line">表单ID：{{ node.intelligentFormFormId || 'form-id' }}</div>
                  <div class="node-summary-line">输出：{{ node.intelligentFormOutputKey || 'formData' }}</div>
                </div>
                <div v-else-if="node.type === 'extractorNodeState'" class="node-summary">
                  <div class="node-summary-line">类型：{{ node.extractorType || 'text' }}</div>
                  <div class="node-summary-line">输出：{{ node.extractorOutputKey || 'extractedData' }}</div>
                </div>
                <div v-else-if="node.type === 'endNodeEnd'" class="node-summary">
                  <div class="node-summary-line">输出：{{ node.outputKey || 'llmOutput' }}</div>
                </div>
                <div v-else class="node-summary">
                  <div class="node-summary-line">输入：inputs</div>
                </div>
              </div>

              <!-- Node-level debug status (last run) -->
              <div v-if="debugHasRun" class="node-debug" @pointerdown.stop>
                <template v-if="traceByNodeId[node.id] && traceByNodeId[node.id].length > 0">
                  <details class="node-debug-details" :open="node.id === selectedNodeId">
                    <summary class="node-debug-summary" @pointerdown.stop>
                      <div class="node-debug-left">
                        <span
                          :class="[
                            'node-debug-icon',
                            traceByNodeId[node.id].at(-1).status === 'success'
                              ? 'is-success'
                              : traceByNodeId[node.id].at(-1).status === 'error'
                                ? 'is-error'
                                : 'is-unknown'
                          ]"
                          aria-hidden="true"
                        >
                          {{ traceByNodeId[node.id].at(-1).status === 'success' ? '✓' : traceByNodeId[node.id].at(-1).status === 'error' ? '!' : '·' }}
                        </span>
                        <span class="node-debug-title">
                          {{ traceByNodeId[node.id].at(-1).status === 'success' ? '运行成功' : traceByNodeId[node.id].at(-1).status === 'error' ? '运行失败' : '运行状态' }}
                          <span class="node-debug-count">(执行 {{ traceByNodeId[node.id].length }} 次)</span>
                        </span>
                        <span v-if="traceByNodeId[node.id].at(-1).startedAt != null && traceByNodeId[node.id].at(-1).finishedAt != null" class="node-debug-time">
                          {{ formatDurationSeconds(traceByNodeId[node.id].at(-1)) }}
                        </span>
                      </div>
                      <div class="node-debug-right">
                        <span class="node-debug-chevron" aria-hidden="true" />
                      </div>
                    </summary>

                    <div class="node-debug-body">
                      <div v-if="traceByNodeId[node.id].at(-1).error" class="node-debug-error">
                        {{ traceByNodeId[node.id].at(-1).error }}
                      </div>

                      <div class="node-debug-label">最新输出</div>
                      <div v-if="node.type === 'textToImageNodeState' && traceByNodeId[node.id].at(-1).output && traceByNodeId[node.id].at(-1).output.url" class="node-debug-image-preview">
                        <img :src="traceByNodeId[node.id].at(-1).output.url" alt="Generated Image" @error="handleImageError" />
                        <div class="node-debug-image-actions">
                          <el-button size="small" type="primary" @click="openImage(traceByNodeId[node.id].at(-1).output.url)">打开图片</el-button>
                          <el-button size="small" @click="copyImageUrl(traceByNodeId[node.id].at(-1).output.url)">复制URL</el-button>
                        </div>
                      </div>
                      <pre v-else class="node-debug-pre">{{ prettyJson(traceByNodeId[node.id].at(-1).output) }}</pre>
                    </div>
                  </details>
                </template>
                <template v-else>
                  <div class="node-debug-empty">未执行</div>
                </template>
              </div>

              <div class="node-port port-in" 
                   @mouseenter="connectingFromId && (hoveredInputNodeId = node.id)"
                   @mouseleave="hoveredInputNodeId = null"
                   :class="{ 'can-connect': connectingFromId && canConnectTo && hoveredInputNodeId === node.id, 'cannot-connect': connectingFromId && !canConnectTo && hoveredInputNodeId === node.id }" 
               />
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
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag required">必需</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.knowledge&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{llmOutputVarName}}</span>
                      <span class="io-desc">大模型生成的文本内容</span>
                    </div>
                  </div>
                </div>
                <div class="model-selection-section">
                  <div class="section-title">调用方式选择</div>
                  <el-radio-group v-model="modelSelectionMode" @change="onModelSelectionModeChange">
                    <el-radio-button label="agent">通过智能体调用</el-radio-button>
                    <el-radio-button label="direct">直接调用大模型</el-radio-button>
                  </el-radio-group>
                </div>

                <!-- Agent Selection -->
                <el-form-item v-if="modelSelectionMode === 'agent'" label="智能体(Agent)">
                  <el-select v-model="selectedNode.llmAgentId" placeholder="请选择智能体" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="agent in availableAgents"
                      :key="agent.id"
                      :label="`${agent.name} (ID: ${agent.id})`"
                      :value="agent.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ agent.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">选择用于大模型调用的智能体</div>
                </el-form-item>

                <!-- Direct Model Selection -->
                <div v-if="modelSelectionMode === 'direct'" class="direct-model-section">
                  <el-form-item label="模型提供商">
                    <el-select v-model="selectedNode.modelProvider" placeholder="请选择模型提供商" style="width: 100%" clearable>
                      <el-option label="qwen" value="qwen" />
                    </el-select>
                  </el-form-item>
                  
                  <el-form-item label="模型名称">
                    <el-select v-model="selectedNode.modelName" placeholder="请选择模型名称" style="width: 100%" clearable>
                      <el-option label="qwen-max-latest" value="qwen-max-latest" />
                      <el-option label="qwen-plus" value="qwen-plus" />
                      <el-option label="qwen-turbo" value="qwen-turbo" />
                      
                      
                      
                      
                    </el-select>
                  </el-form-item>
                  
                  <el-collapse v-model="activeCollapse">
                    <el-collapse-item title="高级参数" name="advanced">
                      <el-form-item label="温度 (Temperature)">
                        <el-slider
                          v-model="selectedNode.modelTemperature"
                          :min="0"
                          :max="2"
                          :step="0.1"
                          :format-tooltip="(val: number) => val.toFixed(1)"
                        />
                        <div class="form-hint">控制输出的随机性，值越高越随机</div>
                      </el-form-item>
                      
                      <el-form-item label="最大令牌数 (Max Tokens)">
                        <el-input-number
                          v-model="selectedNode.modelMaxTokens"
                          :min="1"
                          :max="32768"
                          :step="100"
                          style="width: 100%"
                        />
                        <div class="form-hint">控制生成文本的最大长度</div>
                      </el-form-item>
                      
                      <el-form-item label="Top P">
                        <el-slider
                          v-model="selectedNode.modelTopP"
                          :min="0"
                          :max="1"
                          :step="0.01"
                          :format-tooltip="(val: number) => val.toFixed(2)"
                        />
                        <div class="form-hint">控制核采样的概率阈值</div>
                      </el-form-item>
                    </el-collapse-item>
                  </el-collapse>
                </div>
                <el-form-item label="System Prompt (系统提示词)">
                  <el-input
                    v-model="selectedNode.systemPrompt"
                    type="textarea"
                    :rows="4"
                    resize="none"
                    placeholder="例如：你是一个专业的AI助手，擅长回答问题。"
                  />
                  <div class="form-hint">可选。定义AI的角色和行为，支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="Prompt (用户提示词)">
                  <el-input
                    v-model="selectedNode.prompt"
                    type="textarea"
                    :rows="6"
                    resize="none"
                    placeholder="例如：问题：&lbrace;&lbrace;inputs.query&rbrace;&rbrace;\n知识：&lbrace;&lbrace;vars.knowledge&rbrace;&rbrace;"
                  />
                  <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="输出字段名 (llmOutputKey)">
                  <el-input v-model="selectedNode.llmOutputKey" placeholder="llmOutput" />
                  <div class="form-hint">下游节点可通过 {{llmOutputVarName}} 引用此输出</div>
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
                <div class="node-io-info">
                    <div class="io-section">
                      <div class="io-title">输入变量</div>
                      <div class="io-item">
                        <span class="io-tag optional">可选</span>
                        <span class="io-name">inputs.*</span>
                        <span class="io-desc">工作流输入变量，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                      </div>
                      <div class="io-item">
                        <span class="io-tag optional">可选</span>
                        <span class="io-name">vars.*</span>
                        <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.text&rbrace;&rbrace;</code></span>
                      </div>
                    </div>
                    <div class="io-section">
                      <div class="io-title">配置</div>
                      <div class="io-item">
                        <span class="io-tag config">Agent</span>
                        <span class="io-name">已选择 Agent</span>
                        <span class="io-desc">使用所选 Agent 关联的知识库进行检索</span>
                      </div>
                    </div>
                    <div class="io-section">
                      <div class="io-title">输出变量</div>
                      <div class="io-item">
                        <span class="io-tag output">输出</span>
                        <span class="io-name">{{knowledgeOutputVarName}}</span>
                        <span class="io-desc">检索到的知识内容</span>
                      </div>
                    </div>
                  </div>
                <el-form-item label="Query 模板 (queryTemplate)">
                  <el-input v-model="selectedNode.queryTemplate" type="textarea" :rows="2" placeholder="&lbrace;&lbrace;inputs.query&rbrace;&rbrace; 或 &lbrace;&lbrace;vars.text&rbrace;&rbrace;" />
                    <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 或 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="选择知识库（优先）">
                  <el-select v-model="selectedNode.knowledgeBaseId" placeholder="请选择知识库（推荐）" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="kb in availableKnowledgeBases"
                      :key="kb.id"
                      :label="`${kb.name} (ID: ${kb.id})`"
                      :value="kb.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ kb.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ kb.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">直接选择知识库（推荐），无需创建中间智能体</div>
                </el-form-item>
                <el-form-item label="选择 Agent（备选）">
                  <el-select v-model="selectedNode.agentId" placeholder="请选择 Agent（如果未选择知识库）" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="agent in availableAgents"
                      :key="agent.id"
                      :label="`${agent.name} (ID: ${agent.id})`"
                      :value="agent.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ agent.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">如果未选择知识库，则使用该 Agent 关联的知识库（向后兼容）</div>
                </el-form-item>
                <el-form-item label="输出字段名 (knowledgeOutputKey)">
                  <el-input v-model="selectedNode.knowledgeOutputKey" placeholder="knowledge" />
                  <div class="form-hint">下游节点可通过 {{knowledgeOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'textConcatenationNodeState' || selectedNode.type === 'variableAggregationNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.knowledge&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{textOutputVarName}}</span>
                      <span class="io-desc">拼接后的文本内容</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="模板列表（每行一个）">
                  <el-input v-model="selectedNode.partsText" type="textarea" :rows="8" resize="none" />
                  <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量，如：\n<code>&lbrace;&lbrace;vars.knowledge&rbrace;&rbrace;</code>\n问题：<code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="分隔符 (separator)">
                  <el-input v-model="selectedNode.separator" placeholder="\n" />
                  <div class="form-hint">多个模板之间的分隔符，默认换行符</div>
                </el-form-item>
                <el-form-item label="输出字段名 (textOutputKey)">
                  <el-input v-model="selectedNode.textOutputKey" :placeholder="selectedNode.type === 'variableAggregationNodeState' ? 'result' : 'text'" />
                  <div class="form-hint">下游节点可通过 {{textOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'variableUpdaterNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{updaterTargetVarName}}</span>
                      <span class="io-desc">更新后的变量值</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="目标变量名 (targetKey)">
                  <el-input v-model="selectedNode.targetKey" placeholder="answer" />
                  <div class="form-hint">下游节点可通过 {{updaterTargetVarName}} 引用此输出</div>
                </el-form-item>
                <el-form-item label="值模板 (valueTemplate)">
                  <el-input v-model="selectedNode.valueTemplate" placeholder="&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;" />
                  <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'endNodeEnd'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag required">必需</span>
                      <span class="io-name">{{endInputVarName}}</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code> 或 <code>&lbrace;&lbrace;vars.knowledge&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">output</span>
                      <span class="io-desc">工作流最终输出结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输出字段名 (outputKey)">
                  <el-input v-model="selectedNode.outputKey" placeholder="llmOutput" />
                  <div class="form-hint">指定上游节点的输出变量名，如 llmOutput、knowledge 等</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'conditionNodeCondition'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数</span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量</span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">branchResult</span>
                      <span class="io-desc">匹配的分支结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="条件分支">
                  <div v-for="(branch, branchIndex) in selectedNode.conditionParams?.branches" :key="branch.id" class="condition-branch">
                    <div class="branch-header">
                      <el-select v-model="branch.type" placeholder="分支类型" size="small" style="width: 80px; margin-right: 10px">
                        <el-option label="If" value="if" />
                        <el-option label="Else" value="else" />
                      </el-select>
                      <el-button size="small" type="danger" text @click="removeBranch(branchIndex)" v-if="selectedNode.conditionParams?.branches && selectedNode.conditionParams.branches.length > 1">删除</el-button>
                    </div>
                    <div v-if="branch.type === 'if'" v-for="(cond) in branch.conditions" :key="cond.id" class="condition-item">
                      <el-form-item label="条件类型">
                        <el-select v-model="cond.condition" placeholder="选择条件" style="width: 100%">
                          <el-option label="等于 (equal)" value="equal" />
                          <el-option label="不等于 (notEqual)" value="notEqual" />
                          <el-option label="包含 (contains)" value="contains" />
                          <el-option label="不包含 (notContains)" value="notContains" />
                          <el-option label="大于 (greaterThan)" value="greaterThan" />
                          <el-option label="小于 (lessThan)" value="lessThan" />
                          <el-option label="大于等于 (greaterThanOrEqual)" value="greaterThanOrEqual" />
                          <el-option label="小于等于 (lessThanOrEqual)" value="lessThanOrEqual" />
                          <el-option label="为真 (true)" value="true" />
                          <el-option label="为假 (false)" value="false" />
                        </el-select>
                      </el-form-item>
                      <div v-if="cond.value && cond.value.length >= 2" class="condition-values">
                        <el-form-item label="左侧值">
                          <el-select v-model="cond.value[0].from" placeholder="来源" style="width: 100px">
                            <el-option label="引用" value="Reference" />
                            <el-option label="输入" value="Input" />
                          </el-select>
                          <el-input v-if="cond.value[0].from === 'Input'" v-model="cond.value[0].value as string" placeholder="输入值" style="width: calc(100% - 110px); margin-left: 10px" />
                          <el-select v-else v-model="cond.value[0].referenceKey" placeholder="选择变量" style="width: calc(100% - 110px); margin-left: 10px">
                            <el-option label="llmOutput" value="llmOutput" />
                            <el-option label="knowledge" value="knowledge" />
                            <el-option label="query" value="query" />
                          </el-select>
                        </el-form-item>
                        <el-form-item label="右侧值">
                          <el-select v-model="cond.value[1].from" placeholder="来源" style="width: 100px">
                            <el-option label="引用" value="Reference" />
                            <el-option label="输入" value="Input" />
                          </el-select>
                          <el-input v-if="cond.value[1].from === 'Input'" v-model="cond.value[1].value as string" placeholder="输入值" style="width: calc(100% - 110px); margin-left: 10px" />
                          <el-select v-else v-model="cond.value[1].referenceKey" placeholder="选择变量" style="width: calc(100% - 110px); margin-left: 10px">
                            <el-option label="llmOutput" value="llmOutput" />
                            <el-option label="knowledge" value="knowledge" />
                            <el-option label="query" value="query" />
                          </el-select>
                        </el-form-item>
                      </div>
                    </div>
                  </div>
                  <el-button size="small" type="primary" text @click="addBranch">添加分支</el-button>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'httpNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{httpOutputVarName}}</span>
                      <span class="io-desc">HTTP请求响应结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="请求URL (httpUrl)">
                  <el-input v-model="selectedNode.httpUrl" placeholder="https://api.example.com" />
                  <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="请求方法 (httpMethod)">
                  <el-select v-model="selectedNode.httpMethod" placeholder="选择方法" style="width: 100%">
                    <el-option label="GET" value="GET" />
                    <el-option label="POST" value="POST" />
                    <el-option label="PUT" value="PUT" />
                    <el-option label="DELETE" value="DELETE" />
                    <el-option label="PATCH" value="PATCH" />
                  </el-select>
                </el-form-item>
                <el-form-item label="请求头 (httpHeaders)">
                  <el-input v-model="selectedNode.httpHeaders" type="textarea" :rows="4" placeholder='{"Content-Type": "application/json"}' />
                  <div class="form-hint">JSON格式的请求头配置</div>
                </el-form-item>
                <el-form-item label="请求体 (httpBody)">
                  <el-input v-model="selectedNode.httpBody" type="textarea" :rows="6" placeholder='{"key": "value"}' />
                  <div class="form-hint">支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量，仅POST/PUT/PATCH有效</div>
                </el-form-item>
                <el-form-item label="输出字段名 (httpOutputKey)">
                  <el-input v-model="selectedNode.httpOutputKey" placeholder="httpResponse" />
                  <div class="form-hint">下游节点可通过 {{httpOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'codeNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag external">外部输入</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{codeOutputVarName}}</span>
                      <span class="io-desc">代码执行结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="代码语言 (codeLanguage)">
                  <el-select v-model="selectedNode.codeLanguage" placeholder="选择语言" style="width: 100%">
                    <el-option label="JavaScript" value="javascript" />
                    <el-option label="Python" value="python" />
                  </el-select>
                </el-form-item>
                <el-form-item label="代码脚本 (codeScript)">
                  <el-input v-model="selectedNode.codeScript" type="textarea" :rows="12" placeholder="// 在这里编写你的代码
// 可用变量: inputs, vars
// 返回值将作为输出

return {
  result: 'Hello World'
}" />
                  <div class="form-hint">支持使用 <code>inputs.xxx</code> 和 <code>vars.xxx</code> 访问变量，必须返回一个对象作为输出</div>
                </el-form-item>
                <el-form-item label="输出字段名 (codeOutputKey)">
                  <el-input v-model="selectedNode.codeOutputKey" placeholder="codeResult" />
                  <div class="form-hint">下游节点可通过 {{codeOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'loopNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag external">外部输入</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{loopOutputVarName}}</span>
                      <span class="io-desc">循环执行结果数组</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="循环列表 (loopList)">
                  <el-input v-model="selectedNode.loopList" type="textarea" :rows="4" placeholder='["item1", "item2", "item3"]' />
                  <div class="form-hint">JSON格式的数组，支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="迭代变量名 (loopIterationVar)">
                  <el-input v-model="selectedNode.loopIterationVar" placeholder="item" />
                  <div class="form-hint">每次迭代时的变量名，在循环体内可通过该变量访问当前元素</div>
                </el-form-item>
                <el-form-item label="最大迭代次数 (loopMaxIterations)">
                  <el-input-number v-model="selectedNode.loopMaxIterations" :min="1" :max="1000" style="width: 100%" />
                  <div class="form-hint">防止无限循环的安全限制</div>
                </el-form-item>
                <el-form-item label="输出字段名 (loopOutputKey)">
                  <el-input v-model="selectedNode.loopOutputKey" placeholder="loopResults" />
                  <div class="form-hint">下游节点可通过 {{loopOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>
              <template v-else-if="selectedNode.type === 'parallelNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag external">外部输入</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{parallelOutputVarName}}</span>
                      <span class="io-desc">并行执行结果数组</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="并行任务配置 (parallelCalls)">
                  <el-input v-model="selectedNode.parallelCalls" type="textarea" :rows="10" placeholder='[{"id": "call-1", "name": "任务1", "type": "Object", "from": "Expand", "value": []}]' />
                  <div class="form-hint">JSON格式的并行任务数组，支持使用 <code>&lbrace;&lbrace;inputs.xxx&rbrace;&rbrace;</code> 和 <code>&lbrace;&lbrace;vars.xxx&rbrace;&rbrace;</code> 引用变量</div>
                </el-form-item>
                <el-form-item label="输出字段名 (parallelOutputKey)">
                  <el-input v-model="selectedNode.parallelOutputKey" placeholder="parallelResults" />
                  <div class="form-hint">下游节点可通过 {{parallelOutputVarName}} 引用此输出</div>
                </el-form-item>
              </template>
              <template v-else-if="selectedNode.type === 'manualCheckNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag external">外部输入</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></span>
                    </div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">vars.manualCheckResult</span>
                      <span class="io-desc">人工检查结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="表单ID (formId)">
                  <el-input v-model="selectedNode.manualCheckFormId" placeholder="form-id" />
                  <div class="form-hint">关联的表单ID</div>
                </el-form-item>
                <el-form-item label="表单版本 (formVersion)">
                  <el-input v-model="selectedNode.manualCheckFormVersion" placeholder="1.0" />
                  <div class="form-hint">表单版本号</div>
                </el-form-item>
                <el-form-item label="启用阶段描述">
                  <el-switch v-model="selectedNode.manualCheckEnableStageDesc" />
                  <div class="form-hint">是否显示阶段描述</div>
                </el-form-item>
                <el-form-item label="阶段描述 (stageDesc)" v-if="selectedNode.manualCheckEnableStageDesc">
                  <el-input v-model="selectedNode.manualCheckStageDesc" type="textarea" :rows="3" placeholder="请检查以下内容" />
                  <div class="form-hint">人工检查时的提示信息</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'replyNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">vars.replyResult</span>
                      <span class="io-desc">回复结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="消息类型">
                  <el-select v-model="selectedNode.replyMessageType" placeholder="选择消息类型">
                    <el-option label="文本" value="text" />
                    <el-option label="JSON" value="json" />
                    <el-option label="Markdown" value="markdown" />
                  </el-select>
                  <div class="form-hint">消息内容的格式类型</div>
                </el-form-item>
                <el-form-item label="回复消息">
                  <el-input v-model="selectedNode.replyMessage" type="textarea" :rows="6" placeholder="请输入回复消息，可使用变量引用，如 {{vars.llmOutput}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="启用流式输出">
                  <el-switch v-model="selectedNode.replyEnableStreaming" />
                  <div class="form-hint">是否启用流式输出</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'toolInvokeNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.llmOutput&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ toolOutputVarName }}</span>
                      <span class="io-desc">工具调用结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="工具ID">
                  <el-input v-model="selectedNode.toolId" placeholder="请输入工具ID" />
                  <div class="form-hint">要调用的工具的唯一标识符</div>
                </el-form-item>
                <el-form-item label="工具版本">
                  <el-input v-model="selectedNode.toolVersion" placeholder="请输入工具版本，如 1.0" />
                  <div class="form-hint">工具的版本号</div>
                </el-form-item>
                <el-form-item label="触发模式">
                  <el-select v-model="selectedNode.toolTriggerMode" placeholder="选择触发模式">
                    <el-option label="自动" value="auto" />
                    <el-option label="手动" value="manual" />
                  </el-select>
                  <div class="form-hint">工具调用的触发方式</div>
                </el-form-item>
                <el-form-item label="参数配置">
                  <div class="tool-params-list">
                    <div v-for="(param, index) in selectedNode.toolParameters" :key="index" class="tool-param-item">
                      <el-input v-model="param.name" placeholder="参数名" class="param-name-input" />
                      <el-select v-model="param.type" placeholder="类型" class="param-type-select">
                        <el-option label="String" value="String" />
                        <el-option label="Number" value="Number" />
                        <el-option label="Boolean" value="Boolean" />
                        <el-option label="Array" value="Array" />
                        <el-option label="Object" value="Object" />
                      </el-select>
                      <el-input v-model="param.value" placeholder="值，如 {{vars.input}}" class="param-value-input" />
                      <el-button type="danger" :icon="Delete" circle size="small" @click="removeToolParameter(index)" />
                    </div>
                    <el-button type="primary" :icon="Plus" @click="addToolParameter">添加参数</el-button>
                  </div>
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.input&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.toolOutputVar" placeholder="请输入输出变量名，如 toolResult" />
                  <div class="form-hint">工具调用结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'textToImageNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.text&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ textToImageOutputVarName }}</span>
                      <span class="io-desc">生成的图片URL</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="提示词">
                  <el-input v-model="selectedNode.textToImagePrompt" type="textarea" :rows="3" placeholder="请输入提示词，如 {{inputs.prompt}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;inputs.prompt&rbrace;&rbrace;</code> 或 <code>&lbrace;&lbrace;vars.text&rbrace;&rbrace;</code></div>
                </el-form-item>
                <div class="model-selection-section">
                  <div class="section-title">调用方式选择</div>
                  <el-radio-group v-model="textToImageModelSelectionMode" @change="onTextToImageModelSelectionModeChange">
                    <el-radio-button label="agent">通过智能体调用</el-radio-button>
                    <el-radio-button label="direct">直接调用大模型</el-radio-button>
                  </el-radio-group>
                </div>

                <!-- 智能体选择（agent模式） -->
                <el-form-item v-if="textToImageModelSelectionMode === 'agent'" label="智能体(Agent)">
                  <el-select v-model="selectedNode.textToImageAgentId" placeholder="请选择智能体" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="agent in availableAgents"
                      :key="agent.id"
                      :label="`${agent.name} (ID: ${agent.id})`"
                      :value="agent.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ agent.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">选择用于文本转图片的智能体</div>
                </el-form-item>

                <!-- 直接模型选择（direct模式） -->
                <div v-if="textToImageModelSelectionMode === 'direct'" class="direct-model-section">
                  <el-form-item label="模型提供商">
                    <el-select v-model="selectedNode.textToImageModelProvider" placeholder="请选择模型提供商">
                      <el-option label="dashscope" value="dashscope" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="模型名称">
                    <el-select v-model="selectedNode.textToImageModelName" placeholder="请选择模型名称">
                      <template v-if="selectedNode.textToImageModelProvider === 'dashscope'">
                        <el-option label="wanx-v1" value="wanx-v1" />
                        <el-option label="wanx-v2" value="wanx-v2" />
                        <el-option label="wanx-v3" value="wanx-v3" />
                      </template>
                    </el-select>
                  </el-form-item>
                  <div class="image-size-section">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="宽度 (Width)">
                          <el-input-number v-model="selectedNode.textToImageWidth" :min="256" :max="4096" :step="128" style="width: 100%" />
                          <div class="form-hint">图片宽度，单位像素</div>
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="高度 (Height)">
                          <el-input-number v-model="selectedNode.textToImageHeight" :min="256" :max="4096" :step="128" style="width: 100%" />
                          <div class="form-hint">图片高度，单位像素</div>
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </div>
                </div>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.textToImageOutputKey" placeholder="imageUrl" />
                  <div class="form-hint">生成图片URL的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'fileExtractionNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.file&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ fileExtractionOutputVarName }}</span>
                      <span class="io-desc">提取的文件内容</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输入文件">
                  <el-input v-model="selectedNode.fileExtractionInput" placeholder="请输入文件变量，如 {{vars.file}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.file&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="提取类型">
                  <el-select v-model="selectedNode.fileExtractionType" placeholder="选择提取类型">
                    <el-option label="文本" value="text" />
                    <el-option label="JSON" value="json" />
                    <el-option label="XML" value="xml" />
                    <el-option label="CSV" value="csv" />
                  </el-select>
                  <div class="form-hint">文件内容的提取类型</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.fileExtractionOutputKey" placeholder="请输入输出变量名，如 fileContent" />
                  <div class="form-hint">提取内容的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'questionClassificationNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.query&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ questionClassificationOutputVarName }}</span>
                      <span class="io-desc">分类结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输入问题">
                  <el-input v-model="selectedNode.questionClassificationInput" placeholder="请输入问题变量，如 {{inputs.query}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="分类类别">
                  <el-input v-model="selectedNode.questionClassificationCategories" type="textarea" :rows="6" placeholder="请输入分类类别JSON" />
                  <div class="form-hint">分类类别的JSON数组，每个类别包含id、name和description</div>
                </el-form-item>
                <el-form-item label="分类智能体(Agent)">
                  <el-select v-model="selectedNode.questionClassificationAgentId" placeholder="请选择智能体" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="agent in availableAgents"
                      :key="agent.id"
                      :label="`${agent.name} (ID: ${agent.id})`"
                      :value="agent.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ agent.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">选择用于问题分类的智能体</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.questionClassificationOutputKey" placeholder="请输入输出变量名，如 classification" />
                  <div class="form-hint">分类结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'queryOptimizationNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.query&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ queryOptimizationOutputVarName }}</span>
                      <span class="io-desc">优化后的查询</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输入查询">
                  <el-input v-model="selectedNode.queryOptimizationInput" placeholder="请输入查询变量，如 {{inputs.query}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="优化智能体(Agent)">
                  <el-select v-model="selectedNode.queryOptimizationAgentId" placeholder="请选择智能体" style="width: 100%" clearable filterable>
                    <el-option
                      v-for="agent in availableAgents"
                      :key="agent.id"
                      :label="`${agent.name} (ID: ${agent.id})`"
                      :value="agent.id"
                    >
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>{{ agent.name }}</span>
                        <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-hint">选择用于查询优化的智能体</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.queryOptimizationOutputKey" placeholder="请输入输出变量名，如 optimizedQuery" />
                  <div class="form-hint">优化后查询的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'textExtractionNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.text&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ textExtractionOutputVarName }}</span>
                      <span class="io-desc">提取结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输入文本">
                  <el-input v-model="selectedNode.textExtractionInput" placeholder="请输入文本变量，如 {{vars.text}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.text&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="提取类型">
                  <el-select v-model="selectedNode.textExtractionType" placeholder="选择提取类型">
                    <el-option label="关键词提取" value="keyword" />
                    <el-option label="实体提取" value="entity" />
                    <el-option label="摘要提取" value="summary" />
                    <el-option label="情感分析" value="sentiment" />
                  </el-select>
                  <div class="form-hint">从文本中提取的信息类型</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.textExtractionOutputKey" placeholder="请输入输出变量名，如 extractionResult" />
                  <div class="form-hint">提取结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'evaluationAlgorithmsNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.evaluationData&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ evaluationAlgorithmsOutputVarName }}</span>
                      <span class="io-desc">评估结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="评估数据">
                  <el-input v-model="selectedNode.evaluationAlgorithmsInput" placeholder="请输入评估数据变量，如 {{vars.evaluationData}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.evaluationData&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="评估算法">
                  <el-select v-model="selectedNode.evaluationAlgorithmsAlgorithm" placeholder="选择评估算法">
                    <el-option label="准确率" value="accuracy" />
                    <el-option label="精确率" value="precision" />
                    <el-option label="召回率" value="recall" />
                    <el-option label="F1分数" value="f1" />
                    <el-option label="混淆矩阵" value="confusion_matrix" />
                  </el-select>
                  <div class="form-hint">选择评估算法类型</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.evaluationAlgorithmsOutputKey" placeholder="请输入输出变量名，如 evaluationResult" />
                  <div class="form-hint">评估结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'evaluationTestSetNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.testData&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ evaluationTestSetOutputVarName }}</span>
                      <span class="io-desc">测试集结果</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="测试数据">
                  <el-input v-model="selectedNode.evaluationTestSetInput" placeholder="请输入测试数据变量，如 {{vars.testData}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.testData&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="测试集">
                  <el-select v-model="selectedNode.evaluationTestSetTestSet" placeholder="选择测试集">
                    <el-option label="默认测试集" value="default" />
                    <el-option label="自定义测试集" value="custom" />
                  </el-select>
                  <div class="form-hint">选择测试集类型</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.evaluationTestSetOutputKey" placeholder="请输入输出变量名，如 testSetResult" />
                  <div class="form-hint">测试集结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'evaluationStartNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">触发模式</div>
                    <div class="io-item">
                      <span class="io-tag output">配置</span>
                      <span class="io-name">{{ selectedNode.evaluationStartTriggerMode || 'auto' }}</span>
                      <span class="io-desc">评估流程的触发模式</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="触发模式">
                  <el-select v-model="selectedNode.evaluationStartTriggerMode" placeholder="选择触发模式">
                    <el-option label="自动触发" value="auto" />
                    <el-option label="手动触发" value="manual" />
                  </el-select>
                  <div class="form-hint">选择评估流程的触发模式</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'evaluationEndNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量</span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">vars.{{ selectedNode.evaluationEndOutputKey || 'evaluationResult' }}</span>
                      <span class="io-desc">评估流程的最终输出</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.evaluationEndOutputKey" placeholder="请输入输出变量名，如 evaluationResult" />
                  <div class="form-hint">评估流程最终结果的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else-if="selectedNode.type === 'intelligentFormNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量</span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ intelligentFormOutputVarName }}</span>
                      <span class="io-desc">表单数据</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="表单ID">
                  <el-input v-model="selectedNode.intelligentFormFormId" placeholder="请输入表单ID" />
                  <div class="form-hint">智能表单的唯一标识符</div>
                </el-form-item>
                <el-form-item label="表单版本">
                  <el-input v-model="selectedNode.intelligentFormFormVersion" placeholder="请输入表单版本，如 1.0" />
                  <div class="form-hint">表单的版本号</div>
                </el-form-item>
                <el-form-item label="启用阶段描述">
                  <el-switch v-model="selectedNode.intelligentFormEnableStageDesc" />
                  <div class="form-hint">是否启用阶段描述</div>
                </el-form-item>
                <el-form-item label="阶段描述" v-if="selectedNode.intelligentFormEnableStageDesc">
                  <el-input v-model="selectedNode.intelligentFormStageDesc" type="textarea" :rows="3" placeholder="请输入阶段描述" />
                  <div class="form-hint">表单阶段的描述信息</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.intelligentFormOutputKey" placeholder="请输入输出变量名，如 formData" />
                  <div class="form-hint">表单数据的存储变量名</div>
                </el-form-item>
              </template>



              <template v-else-if="selectedNode.type === 'extractorNodeState'">
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag optional">可选</span>
                      <span class="io-name">vars.*</span>
                      <span class="io-desc">上游节点的输出变量，如 <code>&lbrace;&lbrace;vars.data&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">{{ extractorOutputVarName }}</span>
                      <span class="io-desc">提取的数据</span>
                    </div>
                  </div>
                </div>
                <el-form-item label="输入数据">
                  <el-input v-model="selectedNode.extractorInput" placeholder="请输入数据变量，如 {{vars.data}}" />
                  <div class="form-hint">支持变量引用，如 <code>&lbrace;&lbrace;vars.data&rbrace;&rbrace;</code></div>
                </el-form-item>
                <el-form-item label="提取类型">
                  <el-select v-model="selectedNode.extractorType" placeholder="选择提取类型">
                    <el-option label="文本提取" value="text" />
                    <el-option label="结构化数据提取" value="structured" />
                    <el-option label="表格提取" value="table" />
                    <el-option label="图像提取" value="image" />
                  </el-select>
                  <div class="form-hint">选择数据提取类型</div>
                </el-form-item>
                <el-form-item label="输出变量">
                  <el-input v-model="selectedNode.extractorOutputKey" placeholder="请输入输出变量名，如 extractedData" />
                  <div class="form-hint">提取数据的存储变量名</div>
                </el-form-item>
              </template>

              <template v-else>
                <div class="node-io-info">
                  <div class="io-section">
                    <div class="io-title">输入变量</div>
                    <div class="io-item">
                      <span class="io-tag external">外部输入</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">工作流外部输入参数，如 <code>&lbrace;&lbrace;inputs.query&rbrace;&rbrace;</code>、<code>&lbrace;&lbrace;inputs.agentId&rbrace;&rbrace;</code></span>
                    </div>
                  </div>
                  <div class="io-section">
                    <div class="io-title">输出变量</div>
                    <div class="io-item">
                      <span class="io-tag output">输出</span>
                      <span class="io-name">inputs.*</span>
                      <span class="io-desc">将输入参数传递给下游节点使用</span>
                    </div>
                  </div>
                </div>
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
              <div class="debug-input-mode-toggle">
                <el-radio-group v-model="debugUseForm" size="small">
                  <el-radio-button :value="true">表单输入</el-radio-button>
                  <el-radio-button :value="false">JSON 输入</el-radio-button>
                </el-radio-group>
              </div>

              <div v-if="debugUseForm" class="debug-form-input">
                <div class="sub-title">输入参数</div>
                <el-form label-position="top">
                  <el-form-item label="查询问题 (query)">
                    <el-input v-model="debugForm.query" type="textarea" :rows="3" placeholder="请输入查询问题" />
                  </el-form-item>
                  <el-form-item label="选择 Agent">
                    <el-select v-model="debugForm.agentId" placeholder="请选择 Agent（可选）" style="width: 100%" clearable filterable>
                      <el-option
                        v-for="agent in availableAgents"
                        :key="agent.id"
                        :label="`${agent.name} (ID: ${agent.id})`"
                        :value="agent.id"
                      >
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                          <span>{{ agent.name }}</span>
                          <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ agent.id }}</span>
                        </div>
                      </el-option>
                    </el-select>
                    <div class="form-hint">选择要使用的 Agent，系统会自动使用该 Agent 关联的知识库</div>
                  </el-form-item>
                  <el-form-item label="选择知识库">
                    <el-select v-model="debugForm.knowledgeBaseId" placeholder="请选择知识库（可选）" style="width: 100%" clearable filterable>
                      <el-option
                        v-for="kb in availableKnowledgeBases"
                        :key="kb.id"
                        :label="`${kb.name} (ID: ${kb.id})`"
                        :value="kb.id"
                      >
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                          <span>{{ kb.name }}</span>
                          <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ kb.id }}</span>
                        </div>
                      </el-option>
                    </el-select>
                    <div class="form-hint">选择要使用的知识库，如果已选择 Agent 则会优先使用 Agent 关联的知识库</div>
                  </el-form-item>
                </el-form>
              </div>

              <div v-else class="debug-json-input">
              <el-input
                ref="debugInputsRef"
                v-model="debugInputsText"
                type="textarea"
                :rows="6"
                resize="none"
                placeholder='例如：{"query":"你好"}'
              />
              
              <el-collapse style="margin-top: 12px;">
                <el-collapse-item title="查看输入参数示例模板" name="examples">
                  <div class="example-templates">
                    <div class="example-item">
                      <div class="example-title">知识检索 + LLM 回答</div>
                      <pre class="example-code">{
  "query": "什么是人工智能？",
  "agentId": "123456"
}</pre>
                    </div>
                    <div class="example-item">
                      <div class="example-title">简单 LLM 对话</div>
                      <pre class="example-code">{
  "query": "你好，请介绍一下你自己"
}</pre>
                    </div>
                    <div class="example-item">
                      <div class="example-title">文本转图片</div>
                      <pre class="example-code">{
  "prompt": "一只可爱的小猫在花园里玩耍"
}</pre>
                    </div>
                    <div class="example-item">
                      <div class="example-title">多参数输入</div>
                      <pre class="example-code">{
  "query": "分析以下数据",
  "context": "用户提供了销售数据",
  "agentId": "123456"
}</pre>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
              </div>

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
        <!-- Right Resizer -->
        <div 
          class="resizer resizer-right"
          @pointerdown="startResize('right', $event)"
        />
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElCollapse, ElCollapseItem } from 'element-plus'
import { ArrowLeft, Delete, Plus } from '@element-plus/icons-vue'
import {
  createWorkflow,
  updateWorkflow,
  getWorkflow,
  debugWorkflow,
  publishWorkflow,
  unpublishWorkflow,
  type WorkflowVO
} from '@/api/workflow'
import { listPlugins, type Plugin } from '@/api/plugin'
import { fetchAgents, type AgentVO } from '@/api/agent'
import { fetchKnowledgeBases, type KnowledgeBaseVO } from '@/api/knowledge'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const isSaving = ref(false)
const isRunning = ref(false)
const isPublishing = ref(false)
const workflowStatus = ref<'draft' | 'published'>('draft')

const inspectorMode = ref<'node' | 'debug'>('node')

const availablePlugins = ref<Plugin[]>([])
const availableAgents = ref<AgentVO[]>([])
const availableKnowledgeBases = ref<KnowledgeBaseVO[]>([])

// Model selection
const modelSelectionMode = ref<'agent' | 'direct'>('agent')
const textToImageModelSelectionMode = ref<'agent' | 'direct'>('agent')
const activeCollapse = ref(['advanced'])

// Watch selectedNode changes to update model selection mode automatically will be moved after selectedNode definition





async function fetchPlugins() {
  try {
    const res = await listPlugins()
    availablePlugins.value = (res as any) || []
  } catch (error) {
    console.error('获取插件列表失败', error)
  }
}

async function fetchAgentsList() {
  try {
    const res = await fetchAgents({ pageNo: 1, pageSize: 100 })
    availableAgents.value = res.records || []
  } catch (error) {
    console.error('获取 Agent 列表失败', error)
  }
}

async function fetchKnowledgeBasesList() {
  try {
    const res = await fetchKnowledgeBases()
    availableKnowledgeBases.value = res || []
  } catch (error) {
    console.error('获取知识库列表失败', error)
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

const leftPanelWidth = ref(280)
const rightPanelWidth = ref(360)

const resizeState = reactive({
  isResizing: false,
  direction: null as 'left' | 'right' | null,
  startX: 0,
  startLeftWidth: 0,
  startRightWidth: 0
})

const contentGridStyle = computed(() => {
  const cols: string[] = []
  if (showLeftPanel.value) cols.push(`${leftPanelWidth.value}px`)
  cols.push('1fr')
  if (showRightPanel.value) cols.push(`${rightPanelWidth.value}px`)
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

const debugUseForm = ref(true)
const debugForm = ref<Record<string, any>>({
  query: '',
  agentId: undefined as number | undefined,
  knowledgeBaseId: undefined as number | undefined
})

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

const traceByNodeId = computed<Record<string, WorkflowTraceEvent[]>>(() => {
  const map: Record<string, WorkflowTraceEvent[]> = {}
  for (const evt of lastTrace.value) {
    if (!evt?.nodeId) continue
    if (!map[evt.nodeId]) {
      map[evt.nodeId] = []
    }
    map[evt.nodeId].push(evt)
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

function handleImageError(event: Event): void {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  ElMessage.error('图片加载失败，请检查URL是否有效')
}

function openImage(url: string): void {
  window.open(url, '_blank')
}

async function copyImageUrl(url: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('图片URL已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
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
  | 'conditionNodeCondition'
  | 'httpNodeState'
  | 'codeNodeState'
  | 'loopNodeState'
  | 'parallelNodeState'
  | 'manualCheckNodeState'
  | 'replyNodeState'
  | 'toolInvokeNodeState'
  | 'textToImageNodeState'
  | 'fileExtractionNodeState'
  | 'questionClassificationNodeState'
  | 'queryOptimizationNodeState'
  | 'textExtractionNodeState'
  | 'evaluationAlgorithmsNodeState'
  | 'evaluationTestSetNodeState'
  | 'evaluationStartNodeState'
  | 'evaluationEndNodeState'
  | 'intelligentFormNodeState'

  | 'extractorNodeState'
  | 'endNodeEnd'

interface CanvasNode {
  id: string
  type: NodeType
  x: number
  y: number
  // llm
  llmAgentId?: number
  prompt?: string
  llmOutputKey?: string
  pluginIds?: number[]
  // direct model selection
  modelProvider?: string
  modelName?: string
  modelTemperature?: number
  modelMaxTokens?: number
  modelTopP?: number

  // knowledgeRetrieval
  queryTemplate?: string
  agentId?: number
  knowledgeBaseId?: number  // 直接指定知识库ID（优先使用）
  knowledgeOutputKey?: string

  // textConcatenation / variableAggregation
  partsText?: string
  separator?: string
  textOutputKey?: string

  // variableUpdater
  targetKey?: string
  valueTemplate?: string

  // condition
  conditionParams?: {
    branches: Array<{
      id: string
      conditionRelation: string
      type: string
      runnable: boolean
      conditions: Array<{
        id: string
        condition: string
        value: Array<{
          id: string
          name: string
          type: string
          from: string
          value: string | string[]
          referenceNode?: string
          referenceId?: string
          referenceKey?: string
        }>
      }>
    }>
  }

  // http
  httpUrl?: string
  httpMethod?: string
  httpHeaders?: string
  httpBody?: string
  httpOutputKey?: string

  // code
  codeLanguage?: string
  codeScript?: string
  codeOutputKey?: string

  // loop
  loopList?: string
  loopIterationVar?: string
  loopMaxIterations?: number
  loopOutputKey?: string

  // parallel
  parallelCalls?: string
  parallelOutputKey?: string

  // manualCheck
  manualCheckFormId?: string
  manualCheckFormVersion?: string
  manualCheckEnableStageDesc?: boolean
  manualCheckStageDesc?: string

  // reply
  replyMessage?: string
  replyMessageType?: 'text' | 'json' | 'markdown'
  replyEnableStreaming?: boolean

  // toolInvoke
  toolId?: string
  toolVersion?: string
  toolTriggerMode?: string
  toolParameters?: Array<{
    id: string
    name: string
    type: string
    value: string
  }>
  toolOutputVar?: string

  // textToImage
  textToImagePrompt?: string
  textToImageAgentId?: number
  textToImageModelProvider?: string
  textToImageModelName?: string
  textToImageWidth?: number
  textToImageHeight?: number
  textToImageSteps?: number
  textToImageOutputKey?: string

  // fileExtraction
  fileExtractionInput?: string
  fileExtractionType?: string
  fileExtractionOutputKey?: string

  // questionClassification
  questionClassificationInput?: string
  questionClassificationCategories?: string
  questionClassificationAgentId?: number
  questionClassificationOutputKey?: string

  // queryOptimization
  queryOptimizationInput?: string
  queryOptimizationAgentId?: number
  queryOptimizationOutputKey?: string

  // textExtraction
  textExtractionInput?: string
  textExtractionType?: string
  textExtractionOutputKey?: string

  // evaluationAlgorithms
  evaluationAlgorithmsInput?: string
  evaluationAlgorithmsAlgorithm?: string
  evaluationAlgorithmsOutputKey?: string

  // evaluationTestSet
  evaluationTestSetInput?: string
  evaluationTestSetTestSet?: string
  evaluationTestSetOutputKey?: string

  // evaluationStart
  evaluationStartTriggerMode?: string

  // evaluationEnd
  evaluationEndOutputKey?: string

  // intelligentForm
  intelligentFormFormId?: string
  intelligentFormFormVersion?: string
  intelligentFormEnableStageDesc?: boolean
  intelligentFormStageDesc?: string
  intelligentFormOutputKey?: string



  // extractor
  extractorInput?: string
  extractorType?: string
  extractorOutputKey?: string

  // end
  outputKey?: string
}

interface CanvasEdge {
  from: string
  to: string
  label?: string
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

// Handle model selection mode change
function onModelSelectionModeChange() {
  // Clear the other selection when switching modes
  if (modelSelectionMode.value === 'agent') {
    if (selectedNode.value) {
      selectedNode.value.modelProvider = undefined
      selectedNode.value.modelName = undefined
      selectedNode.value.modelTemperature = undefined
      selectedNode.value.modelMaxTokens = undefined
      selectedNode.value.modelTopP = undefined
    }
  } else {
    if (selectedNode.value) {
      selectedNode.value.llmAgentId = undefined
    }
  }
}

// Handle text-to-image model selection mode change
function onTextToImageModelSelectionModeChange() {
  // Clear the other selection when switching modes
  if (textToImageModelSelectionMode.value === 'agent') {
    if (selectedNode.value) {
      selectedNode.value.textToImageModelProvider = undefined
      selectedNode.value.textToImageModelName = undefined
      selectedNode.value.textToImageWidth = undefined
      selectedNode.value.textToImageHeight = undefined
    }
  } else {
    if (selectedNode.value) {
      selectedNode.value.textToImageAgentId = undefined
    }
  }
}

const hasSelection = computed(() => Boolean(selectedNodeId.value || selectedEdgeKey.value))

const llmOutputVarName = computed(() => `vars.${selectedNode.value?.llmOutputKey || 'llmOutput'}`)
const knowledgeOutputVarName = computed(() => `vars.${selectedNode.value?.knowledgeOutputKey || 'knowledge'}`)
const textOutputVarName = computed(() => `vars.${selectedNode.value?.textOutputKey || (selectedNode.value?.type === 'variableAggregationNodeState' ? 'result' : 'text')}`)
const endInputVarName = computed(() => `vars.${selectedNode.value?.outputKey || 'llmOutput'}`)
const updaterTargetVarName = computed(() => `vars.${selectedNode.value?.targetKey || 'answer'}`)
const httpOutputVarName = computed(() => `vars.${selectedNode.value?.httpOutputKey || 'httpResponse'}`)
const codeOutputVarName = computed(() => `vars.${selectedNode.value?.codeOutputKey || 'codeResult'}`)
const loopOutputVarName = computed(() => `vars.${selectedNode.value?.loopOutputKey || 'loopResults'}`)
const parallelOutputVarName = computed(() => `vars.${selectedNode.value?.parallelOutputKey || 'parallelResults'}`)
const toolOutputVarName = computed(() => `vars.${selectedNode.value?.toolOutputVar || 'toolResult'}`)

const textToImageOutputVarName = computed(() => `vars.${selectedNode.value?.textToImageOutputKey || 'imageResult'}`)

// Watch selectedNode changes to update model selection mode automatically
watch(selectedNode, (newNode) => {
  if (newNode?.type === 'llmNodeState') {
    // Determine the mode based on which fields are populated
    if (newNode.modelProvider && newNode.modelName) {
      modelSelectionMode.value = 'direct'
    } else {
      modelSelectionMode.value = 'agent'
    }
  } else if (newNode?.type === 'textToImageNodeState') {
    // Determine the mode based on which fields are populated
    if (newNode.textToImageModelProvider && newNode.textToImageModelName) {
      textToImageModelSelectionMode.value = 'direct'
    } else {
      textToImageModelSelectionMode.value = 'agent'
    }
  }
}, { immediate: true })

const fileExtractionOutputVarName = computed(() => `vars.${selectedNode.value?.fileExtractionOutputKey || 'fileContent'}`)
const questionClassificationOutputVarName = computed(() => `vars.${selectedNode.value?.questionClassificationOutputKey || 'classification'}`)
const queryOptimizationOutputVarName = computed(() => `vars.${selectedNode.value?.queryOptimizationOutputKey || 'optimizedQuery'}`)
const textExtractionOutputVarName = computed(() => `vars.${selectedNode.value?.textExtractionOutputKey || 'extractionResult'}`)
const evaluationAlgorithmsOutputVarName = computed(() => `vars.${selectedNode.value?.evaluationAlgorithmsOutputKey || 'evaluationResult'}`)
const evaluationTestSetOutputVarName = computed(() => `vars.${selectedNode.value?.evaluationTestSetOutputKey || 'testSetResult'}`)
const intelligentFormOutputVarName = computed(() => `vars.${selectedNode.value?.intelligentFormOutputKey || 'formData'}`)
const extractorOutputVarName = computed(() => `vars.${selectedNode.value?.extractorOutputKey || 'extractedData'}`)

function parallelTaskCount(node: CanvasNode) {
  if (!node.parallelCalls) return 0
  try {
    const calls = JSON.parse(node.parallelCalls)
    return Array.isArray(calls) ? calls.length : 0
  } catch {
    return 0
  }
}

const edges = ref<CanvasEdge[]>([])
const connectingFromId = ref<string | null>(null)
const mousePosition = ref({ x: 0, y: 0 })

const tempEdgePath = computed(() => {
  if (!connectingFromId.value) return ''
  
  const fromNode = nodes.value.find(n => n.id === connectingFromId.value)
  if (!fromNode) return ''
  
  // 起始点：源节点的输出端口
  const x1 = fromNode.x + NODE_SIZE.width
  const y1 = fromNode.y + NODE_SIZE.height / 2
  
  // 终点：鼠标位置
  const x2 = mousePosition.value.x
  const y2 = mousePosition.value.y
  
  // 使用与edgePath相同的曲线计算逻辑
  const dx = Math.max(40, Math.abs(x2 - x1) * 0.45)
  const c1x = x1 + dx
  const c1y = y1
  const c2x = x2 - dx
  const c2y = y2
  
  return `M ${x1} ${y1} C ${c1x} ${c1y}, ${c2x} ${c2y}, ${x2} ${y2}`
})

// 当前悬停的输入端口节点ID
const hoveredInputNodeId = ref<string | null>(null)

// 判断是否可以连接到目标节点
const canConnectTo = computed(() => {
  if (!connectingFromId.value || !hoveredInputNodeId.value) return false
  
  const fromId = connectingFromId.value
  const toId = hoveredInputNodeId.value
  
  // 不能连接到自己
  if (fromId === toId) return false
  
  // 检查是否已经存在相同的连接
  if (edges.value.some(e => e.from === fromId && e.to === toId)) return false
  
  // 检查条件节点的分支数量限制
  const fromNode = nodes.value.find(n => n.id === fromId)
  if (fromNode && fromNode.type === 'conditionNodeCondition') {
    const existingEdges = edges.value.filter(e => e.from === fromId)
    if (existingEdges.length >= 2) return false
    
    // 检查是否尝试连接到已经连接过的节点（即使删除了边，节点可能还在）
    // 这里我们允许连接，因为 completeConnect 会检查重复连接
  }
  
  return true
})

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
  
  window.addEventListener('pointerup', onConnectPointerUp)
}

function onConnectPointerUp(e: PointerEvent) {
  window.removeEventListener('pointerup', onConnectPointerUp)
  
  if (!connectingFromId.value) return
  
  const target = e.target as HTMLElement
  const portIn = target.closest('.port-in') as HTMLElement
  
  if (portIn) {
    const nodeEl = portIn.closest('.canvas-node') as HTMLElement
    if (nodeEl) {
      const nodeId = nodeEl.dataset.nodeId
      if (nodeId) {
        completeConnect(nodeId)
        return
      }
    }
  }
  
  connectingFromId.value = null
  hoveredInputNodeId.value = null
}

function completeConnect(toId: string) {
  const fromId = connectingFromId.value
  if (!fromId) {
    ElMessage.warning('请先从节点的输出端口（右侧）开始拖动连接线')
    return
  }

  if (fromId === toId) {
    ElMessage.warning('不能连接节点到自身')
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    return
  }
  
  if (!nodes.value.some(n => n.id === fromId) || !nodes.value.some(n => n.id === toId)) {
    ElMessage.warning('无效的节点连接')
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    return
  }
  
  const fromNode = nodes.value.find(n => n.id === fromId)
  
  if (edges.value.some(e => e.from === fromId && e.to === toId)) {
    ElMessage.warning('连接已存在')
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    return
  }
  
  if (fromNode && fromNode.type === 'conditionNodeCondition') {
    const existingEdges = edges.value.filter(e => e.from === fromId)
    if (existingEdges.length >= 2) {
      ElMessage.warning('条件节点最多只能连接2个分支')
      connectingFromId.value = null
      hoveredInputNodeId.value = null
      return
    }
    
    const existingLabel = existingEdges.length > 0 ? existingEdges[0].label : null
    const availableLabel = existingLabel === 'true' ? 'False' : 'True'
    
    ElMessageBox.confirm(`请选择条件分支（当前可用：${availableLabel}）`, '条件分支', {
      confirmButtonText: availableLabel,
      cancelButtonText: '取消',
      distinguishCancelAndClose: true,
      type: 'info'
    }).then(() => {
      const label = availableLabel.toLowerCase()
      edges.value = [...edges.value, { from: fromId, to: toId, label }]
      
      if (!fromNode.conditionParams) {
        fromNode.conditionParams = { branches: [] }
      }
      
      const branchType = label === 'true' ? 'if' : 'else'
      const existingBranch = fromNode.conditionParams.branches.find(b => b.type === branchType)
      
      if (!existingBranch) {
        const newBranch = {
          id: createId('branch'),
          conditionRelation: 'and',
          type: branchType,
          runnable: true,
          conditions: [
            {
              id: createId('cond'),
              condition: 'equal',
              value: [
                {
                  id: createId('val-left'),
                  name: 'left',
                  type: 'string',
                  from: 'Reference',
                  value: ['output', 'llmOutput'],
                  referenceNode: '',
                  referenceId: '',
                  referenceKey: 'llmOutput'
                },
                {
                  id: createId('val-right'),
                  name: 'right',
                  type: 'string',
                  from: 'Input',
                  value: '123',
                  referenceNode: '',
                  referenceId: '',
                  referenceKey: ''
                }
              ]
            }
          ]
        }
        if (branchType === 'if') {
          fromNode.conditionParams.branches.unshift(newBranch)
        } else {
          fromNode.conditionParams.branches.push(newBranch)
        }
      }
      
      connectingFromId.value = null
      hoveredInputNodeId.value = null
      ElMessage.success(`节点连接成功 (${availableLabel}分支)`)
    }).catch(() => {
      connectingFromId.value = null
      hoveredInputNodeId.value = null
      ElMessage.info('已取消连接')
    })
  } else if (fromNode && fromNode.type === 'parallelNodeState') {
    // 并行节点允许连接多个后续节点
    edges.value = [...edges.value, { from: fromId, to: toId }]
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    ElMessage.success('节点连接成功')
  } else if (fromNode && fromNode.type === 'loopNodeState') {
    // 循环节点允许连接两个后续节点：一个后续节点和一个循环返回节点
    const existingEdges = edges.value.filter(e => e.from === fromId)
    if (existingEdges.length >= 2) {
      ElMessage.warning('循环节点最多只能连接2个后续节点')
      connectingFromId.value = null
      hoveredInputNodeId.value = null
      return
    }
    edges.value = [...edges.value, { from: fromId, to: toId }]
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    ElMessage.success('节点连接成功')
  } else {
    // 其他节点只能连接一个后续节点
    const existingEdges = edges.value.filter(e => e.from === fromId)
    if (existingEdges.length > 0) {
      ElMessage.warning('该节点只能连接一个后续节点')
      connectingFromId.value = null
      hoveredInputNodeId.value = null
      return
    }
    edges.value = [...edges.value, { from: fromId, to: toId }]
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    ElMessage.success('节点连接成功')
  }
}

function nodeTitle(type: NodeType) {
  if (type === 'startNodeStart') return '开始'
  if (type === 'knowledgeRetrievalNodeState') return '知识检索'
  if (type === 'textConcatenationNodeState') return '文本串联'
  if (type === 'variableAggregationNodeState') return '变量聚合'
  if (type === 'variableUpdaterNodeState') return '变量更新'
  if (type === 'llmNodeState') return '大模型'
  if (type === 'conditionNodeCondition') return '条件'
  if (type === 'httpNodeState') return 'HTTP'
  if (type === 'codeNodeState') return '代码'
  if (type === 'loopNodeState') return '循环'
  if (type === 'parallelNodeState') return '并行'
  if (type === 'manualCheckNodeState') return '人工检查'
  if (type === 'replyNodeState') return '回复'
  if (type === 'toolInvokeNodeState') return '工具调用'
  if (type === 'textToImageNodeState') return '文本转图片'
  if (type === 'fileExtractionNodeState') return '文件提取'
  if (type === 'questionClassificationNodeState') return '问题分类'
  if (type === 'queryOptimizationNodeState') return '查询优化'
  if (type === 'textExtractionNodeState') return '文本提取'
  if (type === 'evaluationAlgorithmsNodeState') return '评估算法'
  if (type === 'evaluationTestSetNodeState') return '评估测试集'
  if (type === 'evaluationStartNodeState') return '评估开始'
  if (type === 'evaluationEndNodeState') return '评估结束'
  if (type === 'intelligentFormNodeState') return '智能表单'

  if (type === 'extractorNodeState') return '提取'
  return '结束'
}

function onPaletteDragStart(type: NodeType, e: DragEvent) {
  e.dataTransfer?.setData('application/x-workflow-node-type', type)
}

function onCanvasDrop(e: DragEvent) {
  const type = (e.dataTransfer?.getData('application/x-workflow-node-type') || '') as NodeType
  if (!type) return
  if (type === 'startNodeStart' && hasStart.value) {
    ElMessage.warning('一个工作流只能有一个开始节点')
    return
  }
  if (type === 'endNodeEnd' && hasEnd.value) {
    ElMessage.warning('一个工作流只能有一个结束节点，请先删除现有的结束节点')
    return
  }

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
  if (type === 'startNodeStart' && hasStart.value) {
    ElMessage.warning('一个工作流只能有一个开始节点')
    return
  }
  if (type === 'endNodeEnd' && hasEnd.value) {
    ElMessage.warning('一个工作流只能有一个结束节点，请先删除现有的结束节点')
    return
  }

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
    node.prompt = '请结合知识回答：\n\n{{vars.knowledge}}\n\n问题：{{inputs.query}}'
    node.llmOutputKey = 'llmOutput'
  }
  if (type === 'knowledgeRetrievalNodeState') {
    node.queryTemplate = '{{inputs.query}}'
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
  if (type === 'conditionNodeCondition') {
    node.conditionParams = {
      branches: [
        {
          id: createId('branch-if'),
          conditionRelation: 'and',
          type: 'if',
          runnable: true,
          conditions: [
            {
              id: createId('cond'),
              condition: 'equal',
              value: [
                {
                  id: createId('val-left'),
                  name: 'left',
                  type: 'string',
                  from: 'Reference',
                  value: ['output', 'llmOutput'],
                  referenceNode: '',
                  referenceId: '',
                  referenceKey: 'llmOutput'
                },
                {
                  id: createId('val-right'),
                  name: 'right',
                  type: 'string',
                  from: 'Input',
                  value: '123',
                  referenceNode: '',
                  referenceId: '',
                  referenceKey: ''
                }
              ]
            }
          ]
        },
        {
          id: createId('branch-else'),
          conditionRelation: 'and',
          type: 'else',
          runnable: true,
          conditions: [
            {
              id: createId('cond-else'),
              condition: 'true',
              value: []
            }
          ]
        }
      ]
    }
  }
  if (type === 'httpNodeState') {
    node.httpUrl = 'https://api.example.com'
    node.httpMethod = 'GET'
    node.httpHeaders = '{}'
    node.httpBody = ''
    node.httpOutputKey = 'httpResponse'
  }
  if (type === 'codeNodeState') {
    node.codeLanguage = 'javascript'
    node.codeScript = '// 在这里编写你的代码\n// 可用变量: inputs, vars\n// 返回值将作为输出\n\nreturn {\n  result: "Hello World"\n}'
    node.codeOutputKey = 'codeResult'
  }
  if (type === 'loopNodeState') {
    node.loopList = '[]'
    node.loopIterationVar = 'item'
    node.loopMaxIterations = 100
    node.loopOutputKey = 'loopResults'
  }
  if (type === 'parallelNodeState') {
    node.parallelCalls = '[\n  {\n    "id": "call-1",\n    "name": "任务1",\n    "type": "Object",\n    "from": "Expand",\n    "value": []\n  }\n]'
    node.parallelOutputKey = 'parallelResults'
  }
  if (type === 'manualCheckNodeState') {
    node.manualCheckFormId = ''
    node.manualCheckFormVersion = '1.0'
    node.manualCheckEnableStageDesc = true
    node.manualCheckStageDesc = '请检查以下内容'
  }
  if (type === 'replyNodeState') {
    node.replyMessage = '{{vars.llmOutput}}'
    node.replyMessageType = 'text'
    node.replyEnableStreaming = false
  }
  if (type === 'toolInvokeNodeState') {
    node.toolId = ''
    node.toolParameters = []
    node.toolTriggerMode = 'auto'
    node.toolOutputVar = 'toolResult'
  }
  if (type === 'textToImageNodeState') {
    node.textToImagePrompt = '{{inputs.prompt}}'
    node.textToImageOutputKey = 'imageUrl'
  }
  if (type === 'fileExtractionNodeState') {
    node.fileExtractionInput = '{{vars.file}}'
    node.fileExtractionType = 'text'
    node.fileExtractionOutputKey = 'fileContent'
  }
  if (type === 'questionClassificationNodeState') {
    node.questionClassificationInput = '{{inputs.query}}'
    node.questionClassificationCategories = '[\n  {\n    "id": "cat-1",\n    "name": "类别1",\n    "description": "类别1的描述"\n  },\n  {\n    "id": "cat-2",\n    "name": "类别2",\n    "description": "类别2的描述"\n  }\n]'
    node.questionClassificationOutputKey = 'classification'
  }
  if (type === 'queryOptimizationNodeState') {
    node.queryOptimizationInput = '{{inputs.query}}'
    node.queryOptimizationOutputKey = 'optimizedQuery'
  }
  if (type === 'textExtractionNodeState') {
    node.textExtractionInput = '{{vars.text}}'
    node.textExtractionType = 'keyword'
    node.textExtractionOutputKey = 'extractionResult'
  }
  if (type === 'evaluationAlgorithmsNodeState') {
    node.evaluationAlgorithmsInput = '{{vars.evaluationData}}'
    node.evaluationAlgorithmsAlgorithm = 'accuracy'
    node.evaluationAlgorithmsOutputKey = 'evaluationResult'
  }
  if (type === 'evaluationTestSetNodeState') {
    node.evaluationTestSetInput = '{{vars.testData}}'
    node.evaluationTestSetTestSet = 'default'
    node.evaluationTestSetOutputKey = 'testSetResult'
  }
  if (type === 'evaluationStartNodeState') {
    node.evaluationStartTriggerMode = 'auto'
  }
  if (type === 'evaluationEndNodeState') {
    node.evaluationEndOutputKey = 'evaluationResult'
  }
  if (type === 'intelligentFormNodeState') {
    node.intelligentFormFormId = 'form-id'
    node.intelligentFormFormVersion = '1.0'
    node.intelligentFormEnableStageDesc = true
    node.intelligentFormStageDesc = '请填写表单'
    node.intelligentFormOutputKey = 'formData'
  }

  if (type === 'extractorNodeState') {
    node.extractorInput = '{{vars.data}}'
    node.extractorType = 'text'
    node.extractorOutputKey = 'extractedData'
  }
  if (type === 'endNodeEnd') {
    node.outputKey = 'llmOutput'
  }

  nodes.value = [...nodes.value, node]
  selectedNodeId.value = node.id
}

function autoLayoutPositions() {
  const y = Math.round(canvasSize.height / 2 - NODE_SIZE.height / 2)
  const startX = 80
  const gap = 120
  // 计算新节点的位置，确保在画布可见范围内
  let x = startX + nodes.value.length * (NODE_SIZE.width + gap)
  
  // 如果x值过大，重置为合理范围
  if (x > canvasSize.width * 2) {
    x = startX
  }
  
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
    const edgeToDelete = edges.value.find(e => edgeKey(e) === key)
    if (edgeToDelete) {
      const fromNode = nodes.value.find(n => n.id === edgeToDelete.from)
      if (fromNode && fromNode.type === 'conditionNodeCondition' && fromNode.conditionParams?.branches) {
        const branchType = edgeToDelete.label === 'true' ? 'if' : 'else'
        const branchIndex = fromNode.conditionParams.branches.findIndex(b => b.type === branchType)
        if (branchIndex !== -1) {
          fromNode.conditionParams.branches.splice(branchIndex, 1)
          // 更新剩余分支的类型
          const remainingBranches = fromNode.conditionParams.branches
          if (remainingBranches.length === 1) {
            remainingBranches[0].type = 'if'
          }
        }
      }
    }
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

function addBranch() {
  if (!selectedNode.value || !selectedNode.value.conditionParams) return
  const newBranch = {
    id: createId('branch'),
    conditionRelation: 'and',
    type: 'if',
    runnable: true,
    conditions: [
      {
        id: createId('cond'),
        condition: 'equal',
        value: [
          {
            id: createId('val-left'),
            name: 'left',
            type: 'string',
            from: 'Reference',
            value: ['output', 'llmOutput'],
            referenceNode: '',
            referenceId: '',
            referenceKey: 'llmOutput'
          },
          {
            id: createId('val-right'),
            name: 'right',
            type: 'string',
            from: 'Input',
            value: '123',
            referenceNode: '',
            referenceId: '',
            referenceKey: ''
          }
        ]
      }
    ]
  }
  selectedNode.value.conditionParams.branches.push(newBranch)
}

function removeBranch(index: number) {
  const node = selectedNode.value
  if (!node || !node.conditionParams) return
  if (node.conditionParams.branches.length <= 1) {
    ElMessage.warning('至少需要保留一个分支')
    return
  }
  const branch = node.conditionParams.branches[index]
  const branchLabel = branch.type === 'if' ? 'true' : 'false'
  edges.value = edges.value.filter(e => !(e.from === node.id && e.label === branchLabel))
  node.conditionParams.branches.splice(index, 1)
  
  // 更新剩余分支的标签
  const remainingBranches = node.conditionParams.branches
  if (remainingBranches.length === 1) {
    const remainingBranch = remainingBranches[0]
    const remainingEdge = edges.value.find(e => e.from === node.id)
    if (remainingEdge) {
      remainingEdge.label = remainingBranch.type === 'if' ? 'true' : 'false'
    }
    // 将剩余分支类型改为if
    remainingBranch.type = 'if'
  }
}

function addToolParameter() {
  if (!selectedNode.value) return
  if (!selectedNode.value.toolParameters) {
    selectedNode.value.toolParameters = []
  }
  selectedNode.value.toolParameters.push({
    id: Date.now().toString(),
    name: '',
    type: 'String',
    value: ''
  })
}

function removeToolParameter(index: number) {
  if (!selectedNode.value || !selectedNode.value.toolParameters) return
  selectedNode.value.toolParameters.splice(index, 1)
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
    window.removeEventListener('pointerup', onConnectPointerUp)
    connectingFromId.value = null
    hoveredInputNodeId.value = null
    return
  }
  selectedNodeId.value = null
  selectedEdgeKey.value = null
  startPan(e)
}

function handleCanvasPointerMove(e: PointerEvent) {
  if (!canvasRef.value) return
  
  const rect = canvasRef.value.getBoundingClientRect()
  mousePosition.value = {
    x: e.clientX - rect.left - view.offsetX,
    y: e.clientY - rect.top - view.offsetY
  }
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

function startResize(direction: 'left' | 'right', e: PointerEvent) {
  resizeState.isResizing = true
  resizeState.direction = direction
  resizeState.startX = e.clientX
  resizeState.startLeftWidth = leftPanelWidth.value
  resizeState.startRightWidth = rightPanelWidth.value
  
  window.addEventListener('pointermove', onResize)
  window.addEventListener('pointerup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function onResize(e: PointerEvent) {
  if (!resizeState.isResizing || !resizeState.direction) return
  
  const deltaX = e.clientX - resizeState.startX
  
  if (resizeState.direction === 'left') {
    const newWidth = Math.min(Math.max(resizeState.startLeftWidth + deltaX, 200), 500)
    leftPanelWidth.value = newWidth
  } else if (resizeState.direction === 'right') {
    const newWidth = Math.min(Math.max(resizeState.startRightWidth - deltaX, 280), 600)
    rightPanelWidth.value = newWidth
  }
}

function stopResize() {
  resizeState.isResizing = false
  resizeState.direction = null
  
  window.removeEventListener('pointermove', onResize)
  window.removeEventListener('pointerup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
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

  // 如果节点坐标异常，使用默认位置
  if (minX === Number.POSITIVE_INFINITY || maxX === Number.NEGATIVE_INFINITY) {
    resetView()
    return
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
        base.llmAgentId = n.llmAgentId
        base.systemPrompt = n.systemPrompt
        base.prompt = n.prompt
        base.llmOutputKey = n.llmOutputKey || 'llmOutput'
        base.pluginIds = n.pluginIds
        base.modelProvider = n.modelProvider
        base.modelName = n.modelName
        base.modelTemperature = n.modelTemperature
        base.modelMaxTokens = n.modelMaxTokens
        base.modelTopP = n.modelTopP
      }
      if (n.type === 'knowledgeRetrievalNodeState') {
        base.queryTemplate = n.queryTemplate
        base.agentId = n.agentId
        base.knowledgeBaseId = n.knowledgeBaseId
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
      if (n.type === 'conditionNodeCondition') {
        base.conditionParams = n.conditionParams
      }
      if (n.type === 'httpNodeState') {
        base.httpUrl = n.httpUrl
        base.httpMethod = n.httpMethod
        base.httpHeaders = n.httpHeaders
        base.httpBody = n.httpBody
        base.httpOutputKey = n.httpOutputKey || 'httpResponse'
      }
      if (n.type === 'codeNodeState') {
        base.codeLanguage = n.codeLanguage
        base.codeScript = n.codeScript
        base.codeOutputKey = n.codeOutputKey || 'codeResult'
      }
      if (n.type === 'loopNodeState') {
        base.loopList = n.loopList
        base.loopIterationVar = n.loopIterationVar
        base.loopMaxIterations = n.loopMaxIterations
        base.loopOutputKey = n.loopOutputKey || 'loopResults'
      }
      if (n.type === 'parallelNodeState') {
      base.parallelCalls = n.parallelCalls
      base.parallelOutputKey = n.parallelOutputKey || 'parallelResults'
    }
    if (n.type === 'manualCheckNodeState') {
      base.manualCheckFormId = n.manualCheckFormId
      base.manualCheckFormVersion = n.manualCheckFormVersion || '1.0'
      base.manualCheckEnableStageDesc = n.manualCheckEnableStageDesc !== undefined ? n.manualCheckEnableStageDesc : true
      base.manualCheckStageDesc = n.manualCheckStageDesc || '请检查以下内容'
    }
    if (n.type === 'replyNodeState') {
        base.replyMessage = n.replyMessage || '{{vars.llmOutput}}'
        base.replyMessageType = n.replyMessageType || 'text'
        base.replyEnableStreaming = n.replyEnableStreaming !== undefined ? n.replyEnableStreaming : false
      }
      if (n.type === 'toolInvokeNodeState') {
        base.toolId = n.toolId || ''
        base.toolVersion = n.toolVersion || '1.0'
        base.toolTriggerMode = n.toolTriggerMode || 'auto'
        base.toolParameters = n.toolParameters || []
        base.toolOutputVar = n.toolOutputVar || 'toolResult'
      }
      if (n.type === 'textToImageNodeState') {
        base.textToImagePrompt = n.textToImagePrompt || '{{vars.text}}'
        base.textToImageWidth = n.textToImageWidth || 512
        base.textToImageHeight = n.textToImageHeight || 512
        base.textToImageSteps = n.textToImageSteps || 50
        base.textToImageOutputKey = n.textToImageOutputKey || 'imageResult'
        base.textToImageAgentId = n.textToImageAgentId
        base.textToImageModelProvider = n.textToImageModelProvider
        base.textToImageModelName = n.textToImageModelName
      }
      if (n.type === 'fileExtractionNodeState') {
        base.fileExtractionInput = n.fileExtractionInput || '{{vars.file}}'
        base.fileExtractionType = n.fileExtractionType || 'text'
        base.fileExtractionOutputKey = n.fileExtractionOutputKey || 'fileContent'
      }
      if (n.type === 'questionClassificationNodeState') {
        base.questionClassificationInput = n.questionClassificationInput || '{{inputs.query}}'
        base.questionClassificationCategories = n.questionClassificationCategories || '[]'
        base.questionClassificationOutputKey = n.questionClassificationOutputKey || 'classification'
      }
      if (n.type === 'queryOptimizationNodeState') {
        base.queryOptimizationInput = n.queryOptimizationInput || '{{inputs.query}}'
        base.queryOptimizationOutputKey = n.queryOptimizationOutputKey || 'optimizedQuery'
      }
      if (n.type === 'textExtractionNodeState') {
        base.textExtractionInput = n.textExtractionInput || '{{vars.text}}'
        base.textExtractionType = n.textExtractionType || 'keyword'
        base.textExtractionOutputKey = n.textExtractionOutputKey || 'extractionResult'
      }
      if (n.type === 'endNodeEnd') {
        base.outputKey = n.outputKey || 'llmOutput'
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
          node.llmAgentId = n.llmAgentId
          node.systemPrompt = n.systemPrompt
          node.prompt = n.prompt || '请结合知识回答：\n\n{{vars.knowledge}}\n\n问题：{{inputs.query}}'
          node.llmOutputKey = n.llmOutputKey || 'llmOutput'
          node.pluginIds = n.pluginIds || []
          node.modelProvider = n.modelProvider
          node.modelName = n.modelName
          node.modelTemperature = n.modelTemperature
          node.modelMaxTokens = n.modelMaxTokens
          node.modelTopP = n.modelTopP
        }
        if (type === 'knowledgeRetrievalNodeState') {
          node.queryTemplate = n.queryTemplate || '{{inputs.query}}'
          node.agentId = n.agentId
          node.knowledgeBaseId = n.knowledgeBaseId
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
        if (type === 'conditionNodeCondition') {
          node.conditionParams = n.conditionParams
        }
        if (type === 'httpNodeState') {
          node.httpUrl = n.httpUrl || 'https://api.example.com'
          node.httpMethod = n.httpMethod || 'GET'
          node.httpHeaders = n.httpHeaders || '{}'
          node.httpBody = n.httpBody || ''
          node.httpOutputKey = n.httpOutputKey || 'httpResponse'
        }
        if (type === 'codeNodeState') {
          node.codeLanguage = n.codeLanguage || 'javascript'
          node.codeScript = n.codeScript || '// 在这里编写你的代码\n// 可用变量: inputs, vars\n// 返回值将作为输出\n\nreturn {\n  result: "Hello World"\n}'
          node.codeOutputKey = n.codeOutputKey || 'codeResult'
        }
        if (type === 'loopNodeState') {
          node.loopList = n.loopList || '[]'
          node.loopIterationVar = n.loopIterationVar || 'item'
          node.loopMaxIterations = n.loopMaxIterations || 100
          node.loopOutputKey = n.loopOutputKey || 'loopResults'
        }
        if (type === 'parallelNodeState') {
          node.parallelCalls = n.parallelCalls || '[\n  {\n    "id": "call-1",\n    "name": "任务1",\n    "type": "Object",\n    "from": "Expand",\n    "value": []\n  }\n]'
          node.parallelOutputKey = n.parallelOutputKey || 'parallelResults'
        }
        if (type === 'manualCheckNodeState') {
          node.manualCheckFormId = n.manualCheckFormId || ''
          node.manualCheckFormVersion = n.manualCheckFormVersion || '1.0'
          node.manualCheckEnableStageDesc = n.manualCheckEnableStageDesc !== undefined ? n.manualCheckEnableStageDesc : true
          node.manualCheckStageDesc = n.manualCheckStageDesc || '请检查以下内容'
        }
        if (type === 'replyNodeState') {
          node.replyMessage = n.replyMessage || '{{vars.llmOutput}}'
          node.replyMessageType = n.replyMessageType || 'text'
          node.replyEnableStreaming = n.replyEnableStreaming !== undefined ? n.replyEnableStreaming : false
        }
        if (type === 'toolInvokeNodeState') {
          node.toolId = n.toolId || ''
          node.toolVersion = n.toolVersion || '1.0'
          node.toolTriggerMode = n.toolTriggerMode || 'auto'
          node.toolParameters = n.toolParameters || []
          node.toolOutputVar = n.toolOutputVar || 'toolResult'
        }
        if (type === 'textToImageNodeState') {
          node.textToImagePrompt = n.textToImagePrompt || '{{vars.text}}'
          node.textToImageWidth = n.textToImageWidth || 512
          node.textToImageHeight = n.textToImageHeight || 512
          node.textToImageSteps = n.textToImageSteps || 50
          node.textToImageOutputKey = n.textToImageOutputKey || 'imageResult'
          node.textToImageAgentId = n.textToImageAgentId
          node.textToImageModelProvider = n.textToImageModelProvider
          node.textToImageModelName = n.textToImageModelName
        }
        if (type === 'fileExtractionNodeState') {
          node.fileExtractionInput = n.fileExtractionInput || '{{vars.file}}'
          node.fileExtractionType = n.fileExtractionType || 'text'
          node.fileExtractionOutputKey = n.fileExtractionOutputKey || 'fileContent'
        }
        if (type === 'questionClassificationNodeState') {
          node.questionClassificationInput = n.questionClassificationInput || '{{inputs.query}}'
          node.questionClassificationCategories = n.questionClassificationCategories || '[\n  {\n    "id": "cat-1",\n    "name": "类别1",\n    "description": "类别1的描述"\n  },\n  {\n    "id": "cat-2",\n    "name": "类别2",\n    "description": "类别2的描述"\n  }\n]'
          node.questionClassificationOutputKey = n.questionClassificationOutputKey || 'classification'
        }
        if (type === 'queryOptimizationNodeState') {
          node.queryOptimizationInput = n.queryOptimizationInput || '{{inputs.query}}'
          node.queryOptimizationOutputKey = n.queryOptimizationOutputKey || 'optimizedQuery'
        }
        if (type === 'endNodeEnd') {
          node.outputKey = n.outputKey || 'llmOutput'
        }
        return node
      })

    // 确保所有节点都在合理的坐标范围内
    if (parsed.length > 0) {
      // 检查是否所有节点都没有有效的坐标
      const allZeroCoords = parsed.every(n => n.x === 0 && n.y === 0)
      
      if (allZeroCoords) {
        // 如果所有节点都没有坐标，进行自动布局
        const y = Math.round(canvasSize.height / 2 - NODE_SIZE.height / 2)
        const startX = 80
        const gap = 120
        const ordered = [...parsed]
        for (let i = 0; i < ordered.length; i++) {
          ordered[i].x = startX + i * (NODE_SIZE.width + gap)
          ordered[i].y = y
        }
      } else {
        // 检查并修复异常坐标的节点
        for (let i = 0; i < parsed.length; i++) {
          const node = parsed[i]
          
          // 确保x坐标在合理范围内
          if (node.x < 0 || node.x > canvasSize.width * 3) {
            node.x = 80 + i * (NODE_SIZE.width + 120)
          }
          
          // 确保y坐标在合理范围内
          if (node.y < 0 || node.y > canvasSize.height * 3) {
            node.y = Math.round(canvasSize.height / 2 - NODE_SIZE.height / 2)
          }
        }
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
    ElMessage.warning('工作流 graph 解析失败，已清空画布')
    nodes.value = []
    edges.value = []
    // 不再自动添加默认模板，让用户手动添加节点
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
    workflowStatus.value = data.status || 'draft'
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

async function handlePublish() {
  if (!workflowId.value) return
  
  // 先保存工作流
  if (!form.value.name.trim()) {
    ElMessage.warning('请先填写工作流名称并保存')
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
  
  // 确保已保存最新内容
  form.value.graph = buildGraphString()
  try {
    await updateWorkflow(workflowId.value, {
      name: form.value.name,
      description: form.value.description,
      graph: form.value.graph
    })
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败，无法发布')
    return
  }
  
  isPublishing.value = true
  try {
    await publishWorkflow(workflowId.value)
    workflowStatus.value = 'published'
    ElMessage.success('发布成功')
  } catch (error) {
    console.error('发布失败', error)
    ElMessage.error('发布失败')
  } finally {
    isPublishing.value = false
  }
}

async function handleUnpublish() {
  if (!workflowId.value) return
  
  isPublishing.value = true
  try {
    await unpublishWorkflow(workflowId.value)
    workflowStatus.value = 'draft'
    ElMessage.success('取消发布成功')
  } catch (error) {
    console.error('取消发布失败', error)
    ElMessage.error('取消发布失败')
  } finally {
    isPublishing.value = false
  }
}

async function runDebug() {
  if (!workflowId.value) {
    ElMessage.warning('请先保存工作流后再调试')
    return
  }

  let inputs: any

  if (debugUseForm.value) {
    inputs = {}
    if (debugForm.value.query) {
      inputs.query = debugForm.value.query
    }
    if (debugForm.value.agentId) {
      inputs.agentId = debugForm.value.agentId
    }
    if (debugForm.value.knowledgeBaseId) {
      inputs.knowledgeBaseId = debugForm.value.knowledgeBaseId
    }
  } else {
    const rawText = (debugInputsText.value || '').trim()
    if (!rawText) {
      ElMessage.warning('请先填写调试输入（JSON），例如：{"query":"你好"} 或 {}')
      await nextTick()
      debugInputsRef.value?.focus?.()
      return
    }

    try {
      inputs = safeParseJson(debugInputsText.value) || {}
    } catch (e: any) {
      ElMessage.error(`输入参数 JSON 格式错误：${e?.message || '无法解析'}`)
      await nextTick()
      debugInputsRef.value?.focus?.()
      return
    }
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
  updateCanvasWidth()
  window.addEventListener('resize', updateCanvasWidth)
  window.addEventListener('keydown', handleKeydown)
  loadWorkflow()
  fetchPlugins()
  fetchAgentsList()
  fetchKnowledgeBasesList()
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

.resizer {
  position: absolute;
  top: 0;
  bottom: 0;
  background-color: var(--border-color);
  transition: background-color var(--transition-base);
  z-index: 10;
}

.resizer:hover {
  background-color: var(--color-primary);
}

.resizer-left {
  right: -2.5px;
  width: 5px;
  cursor: col-resize;
}

.resizer-right {
  left: -2.5px;
  width: 5px;
  cursor: col-resize;
}

.resizer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
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
  position: relative;
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
  overflow-x: auto;
  white-space: nowrap;
}

/* 自定义滚动条样式 */
.canvas-topbar::-webkit-scrollbar {
  height: 6px;
}

.canvas-topbar::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

.canvas-topbar::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: var(--radius-full);
}

.canvas-topbar::-webkit-scrollbar-thumb:hover {
  background: var(--border-color-dark);
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

.canvas-node.node-codeNodeState .node-dot {
  background: var(--color-warning);
}

.canvas-node.node-loopNodeState .node-dot {
  background: var(--color-danger);
}

.canvas-node.node-parallelNodeState .node-dot {
  background: var(--color-warning);
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
  transition: all var(--transition-base) ease;
  cursor: pointer;
}

.node-port:hover {
  width: 16px;
  height: 16px;
  border-color: var(--color-primary);
  background: var(--bg-primary);
  z-index: 10;
}

.node-port.port-out:hover {
  right: -8px;
}

.node-port.port-in:hover {
  left: -8px;
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

/* 连接状态样式 */
.node-port.can-connect {
  border-color: var(--color-success);
  background: var(--color-success);
  box-shadow: 0 0 10px var(--color-success);
}

.node-port.cannot-connect {
  border-color: var(--color-error);
  background: var(--color-error);
  box-shadow: 0 0 10px var(--color-error);
}

/* 临时连接线样式 */
.canvas-link.temp {
  stroke: var(--color-primary-lighter);
  stroke-dasharray: 5, 5;
  stroke-width: 2;
}

.inspector {
  display: flex;
  flex-direction: column;
  position: relative;
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

.node-io-info {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.io-section {
  margin-bottom: var(--spacing-sm);
}

.io-section:last-child {
  margin-bottom: 0;
}

.io-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-xs);
  text-transform: uppercase;
}

.io-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) 0;
}

.io-tag {
  font-size: var(--font-size-xs);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.io-tag.required {
  background: var(--color-error-light);
  color: var(--color-error);
}

.io-tag.optional {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.io-tag.output {
  background: var(--color-success-light);
  color: var(--color-success);
}

.io-tag.external {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.io-name {
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
}

.io-desc {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  flex: 1;
}

.form-hint {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-top: var(--spacing-xs);
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

.debug-input-mode-toggle {
  margin-bottom: var(--spacing-md);
  display: flex;
  justify-content: center;
}

.debug-form-input {
  padding: var(--spacing-md);
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.debug-json-input {
  padding: var(--spacing-md);
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.example-templates {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.example-item {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  background: var(--bg-primary);
}

.example-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.example-code {
  background: var(--bg-secondary);
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  overflow-x: auto;
  margin: 0;
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
}

.node-debug-count {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: normal;
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

.node-debug-image-preview {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.node-debug-image-preview img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.node-debug-image-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.node-debug-empty {
  padding-top: var(--spacing-xs);
  border-top: 1px solid var(--border-color);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.condition-branch {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.branch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
}

.condition-item {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.condition-item:last-child {
  margin-bottom: 0;
}

.condition-values {
  margin-top: var(--spacing-sm);
}

.parallel-task-item {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.parallel-task-item:last-child {
  margin-bottom: 0;
}

.manual-check-stage-desc {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.stage-desc-preview {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  line-height: 1.6;
  white-space: pre-wrap;
}

.stage-desc-placeholder {
  color: var(--text-secondary);
  font-style: italic;
}

.reply-message-preview {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.reply-message-content {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-message-placeholder {
  color: var(--text-secondary);
  font-style: italic;
}

.reply-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.reply-type-badge.text {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.reply-type-badge.json {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.reply-type-badge.markdown {
  background: var(--color-success-light);
  color: var(--color-success);
}

.tool-params-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-param-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.param-name-input {
  flex: 1;
  min-width: 0;
}

.param-type-select {
  width: 100px;
  flex-shrink: 0;
}

.param-value-input {
  flex: 2;
  min-width: 0;
}

.text-to-image-preview {
  margin-top: 8px;
  padding: 8px;
  background: var(--color-bg-2);
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-2);
}
</style>
