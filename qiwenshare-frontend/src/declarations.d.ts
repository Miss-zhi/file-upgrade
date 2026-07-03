/* 第三方模块类型声明（无 @types 包的模块） */

declare module 'markdown-it' {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const MarkdownIt: any
  export default MarkdownIt
}

declare module 'canvas-nest.js' {
  interface CanvasNestOptions {
    color?: string
    pointColor?: string
    count?: number
    opacity?: number
    zIndex?: number
  }
  class CanvasNest {
    constructor(el: HTMLElement, options?: CanvasNestOptions)
    destroy(): void
  }
  export default CanvasNest
}
