# 模拟工作流执行的测试脚本
# 用于验证变量传播和节点执行逻辑

import json
import sys
from collections import deque

def simulate_workflow_execution():
    """模拟工作流执行，验证变量传播逻辑"""
    print("=== 工作流执行模拟 ===")
    
    # 模拟输入
    inputs = {"query": "测试查询"}
    vars = inputs.copy()  # 初始化vars为输入
    node_outputs = {}
    
    print(f"初始vars: {vars}")
    
    # 模拟节点执行顺序（无LLM节点的情况）
    execution_order = ["start", "end"]
    
    for node_type in execution_order:
        print(f"\n执行节点: {node_type}")
        
        if node_type == "start":
            # start节点执行逻辑 - 不修改vars
            event = {
                "status": "success",
                "output": {"inputs": inputs}
            }
            node_outputs["start"] = event["output"]
            print(f"start节点输出: {event['output']}")
            print(f"执行后vars: {vars}")
            
        elif node_type == "end":
            # end节点执行逻辑
            output_key = "answer"  # 默认输出键
            out = vars.get(output_key)
            
            # 如果默认的 answer 没找到，尝试找 llmOutput 作为兜底
            if out is None and output_key == "answer":
                out = vars.get("llmOutput")
                if out is not None:
                    output_key = "llmOutput"
            
            if out is None:
                print(f"错误: 结束节点未找到输出变量: {output_key}。请检查上游节点的输出变量名是否匹配。可用变量有: {list(vars.keys())}")
                return False
            else:
                print(f"end节点找到变量 {output_key}: {out}")
                return True
    
    return True

if __name__ == "__main__":
    simulate_workflow_execution()