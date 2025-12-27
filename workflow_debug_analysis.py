# 工作流调试错误分析脚本
# 模拟工作流执行，找出"结束节点未找到输出变量: answer"错误的根本原因

import json

class WorkflowNode:
    def __init__(self, node_id, node_type, **kwargs):
        self.id = node_id
        self.type = node_type
        self.__dict__.update(kwargs)

class WorkflowDebug:
    def __init__(self):
        self.inputs = {}
        self.vars = {}
        self.node_outputs = {}
        self.trace = []
        self.final_output = None

    def set_inputs(self, inputs):
        """设置工作流输入"""
        self.inputs = inputs
        self.vars = inputs.copy()  # 初始化vars为输入

    def run_start_node(self, node):
        """执行start节点"""
        print(f"执行start节点: {node.id}")
        # start节点不修改vars
        output = {"inputs": self.inputs}
        self.node_outputs[node.id] = output
        return {
            "status": "success",
            "output": output
        }

    def run_llm_node(self, node):
        """执行llm节点"""
        print(f"执行llm节点: {node.id}")
        # 模拟LLM调用结果
        llm_result = {"text": "LLM的回答内容"}
        
        # 设置输出变量
        output_key = node.llmOutputKey if hasattr(node, 'llmOutputKey') and node.llmOutputKey else "llmOutput"
        text_value = llm_result["text"]
        self.vars[output_key] = text_value
        self.vars["llmOutput"] = text_value  # 保留通用别名
        
        self.node_outputs[node.id] = llm_result
        return {
            "status": "success",
            "output": llm_result
        }

    def run_variable_updater_node(self, node):
        """执行variableUpdater节点"""
        print(f"执行variableUpdater节点: {node.id}")
        target_key = node.targetKey if hasattr(node, 'targetKey') and node.targetKey else "answer"
        value_template = node.valueTemplate if hasattr(node, 'valueTemplate') and node.valueTemplate else "{{vars.llmOutput}}"
        
        # 模拟模板渲染
        rendered_value = self.vars.get("llmOutput", "")
        self.vars[target_key] = rendered_value
        
        output = {target_key: rendered_value}
        self.node_outputs[node.id] = output
        return {
            "status": "success",
            "output": output
        }

    def run_end_node(self, node):
        """执行end节点"""
        print(f"执行end节点: {node.id}")
        output_key = node.outputKey if hasattr(node, 'outputKey') and node.outputKey else "answer"
        out = self.vars.get(output_key)
        
        # 如果默认的answer没找到，尝试找llmOutput作为兜底
        if out is None and output_key == "answer":
            out = self.vars.get("llmOutput")
            if out is not None:
                output_key = "llmOutput"
        
        if out is None:
            error_msg = f"结束节点未找到输出变量: {output_key}。请检查上游节点的输出变量名是否匹配。可用变量有: {list(self.vars.keys())}"
            print(f"❌ 错误: {error_msg}")
            return {
                "status": "error",
                "error": error_msg
            }
        else:
            print(f"✅ 成功: 结束节点找到变量 {output_key}: {out}")
            output = {output_key: out}
            self.node_outputs[node.id] = output
            self.final_output = output
            return {
                "status": "success",
                "output": output
            }

    def run_node(self, node):
        """执行单个节点"""
        normalized_type = self.normalize_type(node.type)
        
        if normalized_type == "start":
            return self.run_start_node(node)
        elif normalized_type == "llm":
            return self.run_llm_node(node)
        elif normalized_type == "variableUpdater":
            return self.run_variable_updater_node(node)
        elif normalized_type == "end":
            return self.run_end_node(node)
        else:
            return {
                "status": "error",
                "error": f"Unsupported node type: {node.type}"
            }

    def normalize_type(self, raw_type):
        """标准化节点类型"""
        if not raw_type:
            return "unknown"
        
        type_mapping = {
            "start": "start",
            "startNodeStart": "start",
            "llm": "llm",
            "llmNodeState": "llm",
            "variableUpdater": "variableUpdater",
            "variableUpdaterNodeState": "variableUpdater",
            "end": "end",
            "endNodeEnd": "end"
        }
        
        return type_mapping.get(raw_type, raw_type)

    def run_workflow(self, nodes, execution_order):
        """执行整个工作流"""
        print("=== 工作流执行开始 ===")
        print(f"输入: {self.inputs}")
        print(f"初始vars: {self.vars}")
        print(f"执行顺序: {[node.id for node in execution_order]}")
        
        for node in execution_order:
            print(f"\n--- 执行节点: {node.id} (类型: {node.type}) ---")
            event = self.run_node(node)
            self.trace.append(event)
            print(f"当前vars: {self.vars}")
            
            if event["status"] == "error":
                print("\n=== 工作流执行失败 ===")
                return False
        
        print("\n=== 工作流执行成功 ===")
        print(f"最终输出: {self.final_output}")
        return True

# 测试场景1: 只有start和end节点（模拟用户遇到的错误）
def test_scenario1():
    print("\n\n" + "="*50)
    print("测试场景1: 只有start和end节点")
    print("="*50)
    
    debug = WorkflowDebug()
    debug.set_inputs({"query": "测试查询"})
    
    # 创建节点
    start_node = WorkflowNode("start1", "startNodeStart")
    end_node = WorkflowNode("end1", "endNodeEnd")
    
    # 执行工作流（只有start和end）
    nodes = [start_node, end_node]
    execution_order = [start_node, end_node]
    
    debug.run_workflow(nodes, execution_order)

# 测试场景2: 添加llm节点（解决方案1）
def test_scenario2():
    print("\n\n" + "="*50)
    print("测试场景2: start -> llm -> end（解决方案1）")
    print("="*50)
    
    debug = WorkflowDebug()
    debug.set_inputs({"query": "测试查询"})
    
    # 创建节点
    start_node = WorkflowNode("start1", "startNodeStart")
    llm_node = WorkflowNode("llm1", "llmNodeState")
    end_node = WorkflowNode("end1", "endNodeEnd")
    
    # 执行工作流（start -> llm -> end）
    nodes = [start_node, llm_node, end_node]
    execution_order = [start_node, llm_node, end_node]
    
    debug.run_workflow(nodes, execution_order)

# 测试场景3: 添加variableUpdater节点（解决方案2）
def test_scenario3():
    print("\n\n" + "="*50)
    print("测试场景3: start -> variableUpdater -> end（解决方案2）")
    print("="*50)
    
    debug = WorkflowDebug()
    debug.set_inputs({"query": "测试查询"})
    
    # 创建节点
    start_node = WorkflowNode("start1", "startNodeStart")
    # 手动设置answer变量为query值
    updater_node = WorkflowNode("updater1", "variableUpdaterNodeState", targetKey="answer", valueTemplate="{{inputs.query}}")
    end_node = WorkflowNode("end1", "endNodeEnd")
    
    # 执行工作流（start -> updater -> end）
    nodes = [start_node, updater_node, end_node]
    execution_order = [start_node, updater_node, end_node]
    
    debug.run_workflow(nodes, execution_order)

# 测试场景4: 修改end节点的outputKey为query（解决方案3）
def test_scenario4():
    print("\n\n" + "="*50)
    print("测试场景4: start -> end，end节点outputKey=query（解决方案3）")
    print("="*50)
    
    debug = WorkflowDebug()
    debug.set_inputs({"query": "测试查询"})
    
    # 创建节点
    start_node = WorkflowNode("start1", "startNodeStart")
    # 修改end节点的outputKey为query
    end_node = WorkflowNode("end1", "endNodeEnd", outputKey="query")
    
    # 执行工作流（start -> end）
    nodes = [start_node, end_node]
    execution_order = [start_node, end_node]
    
    debug.run_workflow(nodes, execution_order)

if __name__ == "__main__":
    # 运行所有测试场景
    test_scenario1()  # 错误场景
    test_scenario2()  # 解决方案1
    test_scenario3()  # 解决方案2
    test_scenario4()  # 解决方案3