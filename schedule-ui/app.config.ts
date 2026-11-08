export default defineAppConfig({
  ui: {
    colors: {
      primary: 'teal',
      secondary: 'violet',
      success: 'emerald',
      info: 'sky',
      warning: 'amber',
      error: 'rose',
      neutral: 'stone'
    },
    // Touch-friendly controls below the desktop breakpoint.
    button: {
      slots: { base: 'max-lg:min-h-11 max-lg:min-w-11 justify-center' }
    },
    badge: {
      slots: { base: 'w-fit' }
    },
    input: {
      slots: { base: 'max-lg:min-h-11' }
    },
    select: {
      slots: { base: 'max-lg:min-h-11' }
    },
    textarea: {
      slots: { base: 'max-lg:min-h-11' }
    },
    checkbox: {
      slots: { base: 'max-lg:size-5' }
    },
    pagination: {
      slots: { list: 'flex-wrap justify-center' }
    },
    modal: {
      slots: { content: 'max-sm:max-h-[90dvh]' }
    }
  }
})
