<template>
  <UTable ref="table" v-bind="$attrs" :columns="responsiveColumns" :empty="$t('common.empty')" :ui="ui">
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
  </UTable>
</template>

<script lang="ts" setup generic="T">
import type { TableColumn } from '@nuxt/ui'

/**
 * UTable that turns into a list of cards below the md breakpoint.
 *
 * Column meta options:
 * - `label`: card label, needed when `header` is a render function
 * - `cardTitle`: render the cell unlabelled and emphasised as the card's title
 */
defineOptions({ inheritAttrs: false })

const props = defineProps<{
  columns: TableColumn<T>[]
}>()

const table = useTemplateRef<any>('table')

defineExpose({
  get tableApi() {
    return table.value?.tableApi
  }
})

const responsiveColumns = computed(() => props.columns.map((column: any) => {
  const label = column.meta?.label ?? (typeof column.header === 'string' ? column.header : undefined)
  const labelled = !!label && !column.meta?.cardTitle
  const sortable = typeof column.header === 'function'

  return {
    ...column,
    meta: {
      ...column.meta,
      class: {
        ...column.meta?.class,
        // Sort buttons stay reachable on mobile, plain headers repeat the card labels and are dropped.
        th: [sortable ? 'max-md:block max-md:shrink-0 max-md:p-0' : 'max-md:hidden', column.meta?.class?.th].filter(Boolean).join(' '),
        td: [
          labelled ? 'cell-labelled' : 'max-md:block',
          column.meta?.cardTitle ? 'max-md:text-base max-md:font-semibold max-md:text-highlighted' : '',
          column.meta?.class?.td
        ].filter(Boolean).join(' ')
      },
      style: {
        ...column.meta?.style,
        td: labelled ? { '--cell-label': JSON.stringify(label), ...column.meta?.style?.td } : column.meta?.style?.td
      }
    }
  }
}))

const hasSortableColumn = computed(() => props.columns.some(column => typeof column.header === 'function'))

const ui = computed(() => ({
  root: 'overflow-visible md:overflow-auto md:rounded-xl md:border md:border-default md:bg-default',
  base: 'max-md:block',
  thead: hasSortableColumn.value ? 'max-md:mb-3 max-md:block max-md:overflow-x-auto' : 'max-md:hidden',
  tbody: 'max-md:block max-md:space-y-3 max-md:divide-y-0',
  tr: 'max-md:flex max-md:gap-1 md:data-[expanded=true]:bg-elevated/50 max-md:[tbody_&]:block max-md:[tbody_&]:rounded-xl max-md:[tbody_&]:border max-md:[tbody_&]:border-default max-md:[tbody_&]:bg-default max-md:[tbody_&]:p-3',
  th: 'whitespace-nowrap',
  td: 'text-default max-md:px-1 max-md:py-1.5 max-md:whitespace-normal',
  separator: 'max-md:hidden'
}))
</script>
