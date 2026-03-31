<template>
  <template v-if="hasChildren">
    <li class="tree-node group" :class="{ expanded: isOpen }" @click="toggle">
      <span class="tree-chevron" :class="{ open: isOpen }" aria-hidden="true">
        <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor">
          <path d="M3 1L7 5L3 9V1Z" />
        </svg>
      </span>
      <span class="tree-label">{{ node.label }}</span>
    </li>
    <li v-if="isOpen" class="tree-child-wrap">
      <ul class="tree-children">
        <DatabaseSidebarTreeRow
          v-for="c in node.children"
          :key="String(c.id)"
          :node="c"
          :active-id="activeId"
          :expanded-ids="expandedIds"
          @select="emit('select', $event)"
          @toggle-expand="emit('toggle-expand', $event)"
        />
      </ul>
    </li>
  </template>
  <li
    v-else
    class="tree-node"
    :class="{ active: activeId === node.id, root: isRootLeaf }"
    @click="emit('select', node.id)"
  >
    <span class="tree-chevron placeholder" aria-hidden="true" />
    <span class="tree-label">{{ node.label }}</span>
  </li>
</template>

<script setup>
import { computed } from 'vue'
import DatabaseSidebarTreeRow from './DatabaseSidebarTreeRow.vue'

const props = defineProps({
  node: { type: Object, required: true },
  activeId: { type: [String, Number], default: '' },
  /** @type {Record<string, boolean>} */
  expandedIds: { type: Object, required: true },
  isRootLeaf: { type: Boolean, default: false },
})

const emit = defineEmits(['select', 'toggle-expand'])

const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)

/** 父级用 ref/reactive 维护；为 true 表示展开 */
const isOpen = computed(() => props.expandedIds[String(props.node.id)] === true)

function toggle() {
  emit('toggle-expand', String(props.node.id))
}
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 8px 10px 4px;
  margin: 0 -8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  border-left: 3px solid transparent;
  list-style: none;
}

.tree-node:hover {
  background: #fafafa;
}

.tree-node.active {
  background: #e8f4ff;
  color: #1a5ce6;
  font-weight: 500;
  border-left-color: #1a5ce6;
}

.tree-chevron {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  flex-shrink: 0;
  color: #999;
  transition: transform 0.15s;
}

.tree-chevron.open {
  transform: rotate(90deg);
}

.tree-chevron.placeholder {
  visibility: hidden;
}

.tree-node.group {
  color: #333;
}

.tree-child-wrap {
  margin-left: 18px;
  list-style: none;
}

.tree-children {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>
