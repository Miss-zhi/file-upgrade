"""
main.py —— RAG 教学系统演示脚本
           
运行方式：
    Windows PowerShell:
        $env:PYTHONIOENCODING="utf-8"
        cd d:\桌面\rga
        python main.py
本脚本通过 5 个渐进式演示，展示 RAG 系统的完整工作流程：

    演示1: 文档加载 ——————————— 了解 Document 对象
    演示2: 文本分块 ——————————— 理解两种分块策略
    演示3: 向量嵌入 ——————————— 看向量是什么样子
    演示4: 检索报告 ——————————— 观察检索过程细节
    演示5: 完整问答 ——————————— 端到端 RAG 流水线
"""
发发啊我发我   阿瓦我发啊阿达啊  阿达的啊啊大苏打
import os
import sys

# 确保能找到 rag 包（从项目根目录运行时自动处理）
_ROOT = os.path.dirname(os.path.abspath(__file__))
if _ROOT not in sys.path:
    sys.path.insert(0, _ROOT)


def print_section(title: str) -> None:
    """打印分节标题"""
    print("\n" + "=" * 60)
    print(f"  {title}")
    print("=" * 60)


def demo_1_document_loading():
    """
    演示1：文档加载
    展示 DocumentLoader 如何将文件/字符串转化为 Document 对象
    """
    print_section("演示1：文档加载（Document Loading）")
    print("RAG 第一步：将知识库内容加载为结构化的 Document 对象\n")

    from rag.document_loader import DocumentLoader

    loader = DocumentLoader()

    # 方式1：从文件加载
    print("【方式1】从文件加载（data/sample.txt）：")
    sample_path = os.path.join(_ROOT, "data", "sample.txt")
    doc = loader.load_file(sample_path)
    # 兼容新版（有 source 属性）和旧版（source 在 metadata 中）
    src = getattr(doc, 'source', doc.metadata.get('source', '未知'))
    print(f"  来源: {src}")
    print(f"  内容长度: {len(doc.content)} 字符")
    print(f"  元数据键: {list(doc.metadata.keys())}")
    print(f"  内容前120字符: {doc.content[:120]}...")

    # 方式2：从字符串加载（兼容新旧 API）
    print("\n【方式2】从字符串创建文档：")    
    text_content = "Python 是一种高级编程语言，以简洁易读著称。它广泛应用于数据科学、Web 开发和人工智能领域。"
    if hasattr(loader, 'load_text'):
        doc2 = loader.load_text(text_content, source="手动输入")
    else:
        doc2 = loader.load_from_string(text_content, "手动输入")
    src2 = getattr(doc2, 'source', doc2.metadata.get('source', '手动输入'))
    print(f"  来源: {src2}")
    print(f"  内容: {doc2.content[:60]}...")

    print("\n✓ Document 对象统一了来源，后续处理无需关心数据从哪里来")
    return doc


def demo_2_text_splitting(doc):
    """
    演示2：文本分块
    对比固定分块和滑动窗口分块的差异
    """
    print_section("演示2：文本分块（Text Splitting）")
    print("为什么要分块？嵌入模型有输入长度限制，块太大检索精度低\n")

    from rag.text_splitter import TextSplitter

    splitter = TextSplitter(chunk_size=200, chunk_overlap=40)

    # 固定分块
    fixed_chunks = splitter.split_document(doc, strategy="fixed")
    fixed_stats = splitter.get_stats(fixed_chunks)
    print(f"【固定分块】chunk_size=200, overlap=0 → 产生 {fixed_stats['count']} 个块：")
    print(f"  平均大小: {fixed_stats['avg_size']:.0f} 字符")
    for chunk in fixed_chunks[:3]:
        # chunk_index 在属性或 metadata 中（兼容新旧版）
        idx = getattr(chunk, 'chunk_index', chunk.metadata.get('chunk_index', '?'))
        print(f"  块{idx}: {chunk.content[:60]}...")

    # 滑动窗口分块
    sliding_chunks = splitter.split_document(doc, strategy="sliding")
    sliding_stats = splitter.get_stats(sliding_chunks)
    print(f"\n【滑动窗口分块】chunk_size=200, overlap=40 → 产生 {sliding_stats['count']} 个块：")
    print(f"  平均大小: {sliding_stats['avg_size']:.0f} 字符")

    # 展示相邻两个块的重叠效果
    if len(sliding_chunks) >= 2:
        print(f"\n  相邻块的重叠对比（体现 overlap=40 的效果）：")
        print(f"  块0（后40字）：...{sliding_chunks[0].content[-40:]}")
        print(f"  块1（前40字）：{sliding_chunks[1].content[:40]}...")

    print(f"\n✓ 滑动分块产生更多块（{sliding_stats['count']} > {fixed_stats['count']}），"
          f"但边界语义更完整")
    print("✓ 推荐使用滑动窗口分块减少语义截断")

    return sliding_chunks


def demo_3_embedding(chunks):
    """
    演示3：向量嵌入
    展示文本如何被转化为向量，以及语义相似性
    """
    print_section("演示3：向量嵌入（Text Embedding）")
    print("向量嵌入将文本映射到高维语义空间，语义相近的文本向量距离更近\n")

    from rag.embedder import TFIDFEmbedder
    from rag.vector_store import VectorStore
    import numpy as np

    # 用所有分块的文本进行训练
    all_texts = [c.content for c in chunks]

    embedder = TFIDFEmbedder()
    embedder.fit(all_texts)           # 建立词汇表
    vectors = embedder.embed_texts(all_texts)

    print(f"词汇表大小: {len(embedder.vocabulary)} 个词")
    print(f"向量矩阵形状: {vectors.shape}（{len(all_texts)} 个块 × {vectors.shape[1]} 维）\n")

    # 演示语义相似度：用几个典型文本
    demo_texts = [
        "Python是一种编程语言",      # ← 与下一条语义相近
        "Python是高级程序设计语言",   # ← 与上一条语义相近
        "今天天气晴朗很好",            # ← 与上两条语义不同
        "RAG技术结合检索与生成",       # ← 与前三条语义不同
    ]
    demo_vecs = embedder.embed_texts(demo_texts)

    print("相似度计算结果（余弦相似度，越高表示语义越相近）：")
    store_temp = VectorStore()
    pairs = [(0, 1, "语义相近"), (0, 2, "语义不同"), (1, 3, "语义不同")]
    for i, j, label in pairs:
        sim = store_temp.similarity(demo_vecs[i], demo_vecs[j])
        print(f"  \"{demo_texts[i][:15]}\" vs \"{demo_texts[j][:15]}\": "
              f"相似度={sim:.4f}  ← {label}")

    print("\n✓ 语义相近的文本相似度高（接近1），语义不同的相似度低（接近0）")
    print("✓ 这就是 RAG 检索的数学基础：用向量距离衡量语义相关性")

    # 将所有分块添加到向量库，供演示4使用
    vec_store = VectorStore()
    vec_store.add_chunks(chunks, vectors)

    return embedder, vec_store


def demo_4_retrieval(embedder, vec_store):
    """
    演示4：向量检索
    展示 Retriever 如何根据问题找到相关文档块
    """
    print_section("演示4：向量检索（Retrieval）")
    print("余弦相似度公式：cos(θ) = (A·B) / (|A|·|B|)")
    print("值域 [-1, 1]，越接近 1 说明两个文本语义越相似\n")

    from rag.retriever import Retriever

    retriever = Retriever(vector_store=vec_store, embedder=embedder, top_k=3)

    test_queries = [
        "Python 是什么编程语言",
        "RAG 检索增强生成技术",
        "向量嵌入余弦相似度",
    ]

    for query in test_queries:
        print(f"查询：{query}")
        results = retriever.retrieve(query)
        for r in results:
            # 兼容不同版本的 SearchResult 字段
            rank = getattr(r, 'rank', '?')
            score = getattr(r, 'score', 0.0)
            chunk = getattr(r, 'chunk', None)
            content = chunk.content[:70] if chunk else str(r)
            print(f"  #{rank} 相似度={score:.4f} | {content}...")
        print()

    return retriever


def demo_5_full_pipeline():
    """
    演示5：完整 RAG 流水线
    展示端到端的问答流程（索引 → 检索 → 生成）
    """
    print_section("演示5：完整 RAG 问答流水线（Full Pipeline）")
    print("将所有组件串联起来，体验完整的 RAG 问答！\n")
    print("流程：文档 → 分块 → 嵌入 → 向量库（索引）")
    print("      问题 → 检索 → 提示词 → LLM → 答案（查询）\n")

    from rag.pipeline import RAGPipeline

    # 初始化流水线（使用 Mock LLM，无需 API Key）
    print("初始化 RAG 系统...")
    pipeline = RAGPipeline(
        chunk_size=300,
        chunk_overlap=50,
        top_k=3,
    )

    # 索引知识库
    sample_path = os.path.join(_ROOT, "data", "sample.txt")
    pipeline.index_file(sample_path)

    status = pipeline.get_status()
    print(f"\n系统状态:")
    print(f"  嵌入器: {status['embedder_type']}")
    print(f"  LLM: {status['llm_type']}")
    print(f"  向量库大小: {status['total_chunks']} 个块")
    print(f"  分块策略: {status['split_strategy']}, chunk_size={status['chunk_size']}")

    # 问答演示
    questions = [
        "什么是RAG技术？它解决了什么问题？",
        "文本分块有哪些策略？",
        "向量数据库有哪些常见产品？",
        "深度学习和机器学习有什么关系？",
    ]

    print("\n" + "─" * 60)
    print("开始问答演示（使用 MockLLMClient，基于关键词匹配）")
    print("─" * 60)

    for i, question in enumerate(questions, start=1):
        print(f"\n【问题{i}】{question}")
        result = pipeline.ask_with_sources(question)

        # ask_with_sources 返回 dict，含 question, answer, sources
        answer = result.get('answer', str(result))
        sources = result.get('sources', [])

        print(f"【回答】\n{answer}")
        print(f"\n【引用来源】共 {len(sources)} 个相关片段：")
        for src in sources:
            rank = src.get('rank', '?')
            score = src.get('score', 0.0)
            src_file = src.get('source_file', '?')
            chunk_id = src.get('chunk_id', '?')
            print(f"  - #{rank} 相似度 {score:.4f} | {src_file} | {chunk_id}")
        print("─" * 60)

    print("\n" + "=" * 60)
    print("  演示完成！")
    print("  你已经了解了 RAG 的完整工作流程：")
    print("  文档加载 → 文本分块 → 向量嵌入 → 相似度检索 → 提示词构建 → LLM生成")
    print("")
    print("  要使用真实 LLM，请参考 rag/llm_client.py 中的 OpenAILLMClient")
    print("  要使用高质量嵌入，请安装: pip install sentence-transformers")
    print("=" * 60)


def main():
    """主函数 —— 依次运行所有演示"""
    print("=" * 60)
    print("  RAG（检索增强生成）教学系统演示")
    print("  Retrieval-Augmented Generation Tutorial")
    print("=" * 60)
    print("\n本演示将带你了解 RAG 系统的每个核心组件：")
    print("  1. 文档加载（Document Loading）")
    print("  2. 文本分块（Text Splitting）")
    print("  3. 向量嵌入（Text Embedding）")
    print("  4. 向量检索（Retrieval）")
    print("  5. 完整流水线（Full Pipeline Q&A）")

    # 检查示例文件是否存在
    sample_path = os.path.join(_ROOT, "data", "sample.txt")
    if not os.path.exists(sample_path):
        print(f"\n[错误] 示例文件不存在: {sample_path}")
        print("请确保 data/sample.txt 文件存在。")
        sys.exit(1)

    try:
        # 演示1：文档加载
        doc = demo_1_document_loading()

        # 演示2：文本分块
        chunks = demo_2_text_splitting(doc)

        # 演示3：向量嵌入
        embedder, vec_store = demo_3_embedding(chunks)

        # 演示4：向量检索
        demo_4_retrieval(embedder, vec_store)

        # 演示5：完整流水线
        demo_5_full_pipeline()

    except Exception as e:
        print(f"\n[错误] 演示过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        print("\n提示：请确保已安装依赖：pip install numpy")


if __name__ == "__main__":
    main()
