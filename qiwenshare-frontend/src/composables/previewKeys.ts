import type { InjectionKey } from 'vue'
import type { useImagePreview } from '@/composables/useImagePreview'
import type { useVideoPreview } from '@/composables/useVideoPreview'
import type { useAudioPreview } from '@/composables/useAudioPreview'
import type { useCodePreview } from '@/composables/useCodePreview'
import type { useMarkdownPreview } from '@/composables/useMarkdownPreview'

/**
 * 预览 composable 注入 keys。
 * FileView 通过 provide 共享 composable 实例，
 * 各预览组件通过 inject 获取同一个实例，避免重复创建。
 */

export const IMAGE_PREVIEW_KEY: InjectionKey<ReturnType<typeof useImagePreview>> = Symbol('imagePreview')
export const VIDEO_PREVIEW_KEY: InjectionKey<ReturnType<typeof useVideoPreview>> = Symbol('videoPreview')
export const AUDIO_PREVIEW_KEY: InjectionKey<ReturnType<typeof useAudioPreview>> = Symbol('audioPreview')
export const CODE_PREVIEW_KEY: InjectionKey<ReturnType<typeof useCodePreview>> = Symbol('codePreview')
export const MARKDOWN_PREVIEW_KEY: InjectionKey<ReturnType<typeof useMarkdownPreview>> = Symbol('markdownPreview')
