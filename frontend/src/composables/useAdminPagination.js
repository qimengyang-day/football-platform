import { ref, nextTick } from 'vue'

export function useAdminPagination(defaultPageSize = 10) {
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  const resetToFirstPage = () => {
    pageNum.value = 1
  }

  const applyPageResult = (data = {}, listKey = 'list') => {
    const t = Number(data?.total ?? 0)
    total.value = t
    return {
      list: data[listKey] || [],
      total: t
    }
  }

  const handlePageChange = async (currentPage, fetcher) => {
    pageNum.value = currentPage
    if (typeof fetcher === 'function') {
      await fetcher()
    }
  }

  const handleSizeChange = async (size, fetcher) => {
    pageSize.value = size
    pageNum.value = 1
    // 与 el-pagination 的 v-model 同一 tick 内对齐，避免请求仍读到旧 pageSize
    await nextTick()
    if (typeof fetcher === 'function') {
      await fetcher()
    }
  }

  /** 删除一条后刷新列表：若当前页已超出最大页则自动回退 */
  const afterDeleteRefresh = async (fetcher) => {
    if (typeof fetcher !== 'function') return
    await fetcher()
    const ps = pageSize.value || 10
    const maxPage = Math.max(1, Math.ceil((total.value || 0) / ps))
    if (pageNum.value > maxPage) {
      pageNum.value = maxPage
      await fetcher()
    }
  }

  return {
    pageNum,
    pageSize,
    total,
    resetToFirstPage,
    applyPageResult,
    handlePageChange,
    handleSizeChange,
    afterDeleteRefresh
  }
}
