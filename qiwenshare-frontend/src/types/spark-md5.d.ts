declare module 'spark-md5' {
  class SparkMD5 {
    append(str: string): void
    appendBinary(binary: string): void
    end(raw?: boolean): string
    reset(): void
    destroy(): void
  }

  namespace SparkMD5 {
    class ArrayBuffer {
      append(arr: globalThis.ArrayBuffer): void
      end(raw?: boolean): string
      reset(): void
      destroy(): void
    }
  }

  export default SparkMD5
}
