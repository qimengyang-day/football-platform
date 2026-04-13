<template>
  <div class="ppu">
    <h1>球员档案修改审核</h1>

    <el-card>
      <template #header>
        <div class="card-header">
          <div class="left">
            <el-select v-model="status" placeholder="状态" style="width: 160px" @change="onStatusChange">
              <el-option label="全部" value="" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>
          </div>
          <div class="right">
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" style="width: 100%">
        <el-table-column prop="playerUserId" label="球员用户 ID" width="120" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="position" label="位置" width="100" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="nationality" label="国籍" width="120" />
        <el-table-column prop="marketValue" label="身价" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="tagType(scope.row.status)">
              {{ statusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminRemark" label="备注" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <template v-if="scope.row.status === 'PENDING'">
              <el-button type="success" size="small" @click="audit(scope.row.id, 'APPROVE')">通过</el-button>
              <el-button type="danger" size="small" @click="audit(scope.row.id, 'REJECT')">拒绝</el-button>
            </template>
            <template v-else>
              <el-button size="small" disabled>已处理</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pgPageNum"
          v-model:page-size="pgPageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pgTotal"
          @size-change="(size) => pg.handleSizeChange(size, load)"
          @current-change="(page) => pg.handlePageChange(page, load)"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminPagination } from '@/composables/useAdminPagination'

const status = ref('')
const records = ref([])
const pg = useAdminPagination()
const pgPageNum = pg.pageNum
const pgPageSize = pg.pageSize
const pgTotal = pg.total

const tagType = (s) => {
  if (s === 'PENDING') return 'warning'
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}
const statusText = (s) => {
  if (s === 'PENDING') return '待审核'
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '已拒绝'
  return s || '-'
}

const onStatusChange = () => {
  pg.resetToFirstPage()
  load()
}

const load = async () => {
  try {
    const res = await axios.get('/api/admin/player/profile-updates', {
      params: { pageNum: pg.pageNum.value, pageSize: pg.pageSize.value, status: status.value }
    })
    if (res.data.code === 200) {
      const result = pg.applyPageResult(
        { total: res.data.data.total, records: res.data.data.records },
        'records'
      )
      records.value = result.list
    } else {
      ElMessage.error(res.data.message || '获取列表失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取列表失败')
  }
}

const audit = async (id, action) => {
  try {
    const remark = await ElMessageBox.prompt('请输入审核备注（可选）', action === 'APPROVE' ? '通过申请' : '拒绝申请', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '备注'
    }).then(r => r.value).catch(() => null)

    if (remark === null) return
    const res = await axios.put(`/api/admin/player/profile-updates/${id}/audit`, { action, remark })
    if (res.data.code === 200) {
      ElMessage.success('操作成功')
      await pg.afterDeleteRefresh(load)
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.ppu {
  padding: 20px;
}
.ppu h1 {
  margin-bottom: 16px;
  color: #333;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

