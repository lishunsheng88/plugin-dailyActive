<script lang="ts" setup>
import { VCard, VPageHeader, VPagination, VTabbar } from "@halo-dev/components"
import { axiosInstance } from "@halo-dev/api-client"
import { onMounted, ref, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { JsonViewer } from 'vue3-json-viewer'
import 'vue3-json-viewer/dist/index.css'

// Types
interface ListItem {
  value: string
  label: string
}

interface RuleInfo {
  spec: {
    id: number
    isInclude: boolean
    rule: string
    version: string
  }
  apiVersion: string
  kind: string
  metadata: {
    name: string
    creationTimestamp: string
    version: number
  }
}

interface RuleTableItem {
  id: string
  rule: string
  type: 'include' | 'exclude'
  creationTimestamp: string
  name: string
}

// Navigation
const navigationItems = [
  { id: "1", label: "接口日志", icon: "icon-log" },
  { id: "2", label: "基本设置", icon: "icon-log" },
]
const activeNavId = ref("1")

// Watch navigation changes
watch(activeNavId, (newId) => {
  if (newId === "2") {
    getRules()
  }
})

// 也可以在模板中直理
const handleNavChange = (id: string) => {
  activeNavId.value = id
  if (id === "2") {
    getRules()
  }
}

// Table State
const logTableData = ref<any>([])
const isTableLoading = ref(true)

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)

// Search Filters
const searchDateRange = ref('')
const selectedUsers = ref<string[]>([])
const selectedClientIps = ref<string[]>([])
const selectedPaths = ref<string[]>([])

// Filter Options
const userOptions = ref<ListItem[]>([])
const clientIpOptions = ref<ListItem[]>([])
const pathOptions = ref<ListItem[]>([])
const isUserOptionsLoading = ref(false)
const isClientIpOptionsLoading = ref(false)
const isPathOptionsLoading = ref(false)

// Date Shortcuts
const dateShortcuts = [
  {
    text: 'Last week',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      return [start, end]
    },
  },
  {
    text: 'Last month',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setMonth(start.getMonth() - 1)
      return [start, end]
    },
  },
  {
    text: 'Last 3 months',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setMonth(start.getMonth() - 3)
      return [start, end]
    },
  },
]

// Detail Dialog
const detailDialogVisible = ref(false)
const currentDetail = ref<any>(null)

// Search Methods
const searchUsers = async (query: string) => {
  if (!query) {
    userOptions.value = []
    return
  }
  
  isUserOptionsLoading.value = true
  try {
    const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/users?username=${query}`)
    userOptions.value = response.data
  } catch (error) {
    console.error("Error fetching users:", error)
  } finally {
    isUserOptionsLoading.value = false
  }
}

const searchClientIps = async (query: string) => {
  if (!query) {
    clientIpOptions.value = []
    return
  }

  isClientIpOptionsLoading.value = true
  try {
    const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/clientIps?clientIp=${query}`)
    clientIpOptions.value = response.data
  } catch (error) {
    console.error("Error fetching client IPs:", error)
  } finally {
    isClientIpOptionsLoading.value = false
  }
}

const searchPaths = async (query: string) => {
  if (!query) {
    pathOptions.value = []
    return
  }

  isPathOptionsLoading.value = true
  try {
    const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/paths?path=${query}`)
    pathOptions.value = response.data
  } catch (error) {
    console.error("Error fetching paths:", error)
  } finally {
    isPathOptionsLoading.value = false
  }
}

// Data Loading
async function fetchLogData(pageNum: number) {
  const searchParams = {
    page: pageNum,
    size: pageSize.value,
    username: selectedUsers.value.map(user => user.substring(1)),
    clientIp: selectedClientIps.value,
    path: selectedPaths.value,
    accessTimes: searchDateRange.value || [],
  }

  isTableLoading.value = true
  try {
    const response = await axiosInstance.post(
      `/apis/dailyActive.halo.run/v1alpha1/interfaceLog/search`, 
      removeCircularReferences(searchParams)
    )
    const { page, size, total, items } = response.data
    
    currentPage.value = page
    pageSize.value = size
    totalItems.value = total
    logTableData.value = items.map(mapLogItem)
  } catch (error) {
    console.error('Error fetching log data:', error)
    ElMessage.error('获取日志数据失败')
  } finally {
    isTableLoading.value = false
  }
}

// Event Handlers
async function handlePageChange(newPage: number) {
  await fetchLogData(newPage)
}

async function handleDetailView(row: any) {
  try {
    const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLogInfos/${row.id}`)
    currentDetail.value = response.data.spec
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

// Utility Functions
function mapLogItem(item: any) {
  return {
    id: item.id,
    accessTime: item.accessTime,
    username: item.username,
    clientIp: item.clientIp,
    path: item.path,
    requestType: item.requestType,
    responseStatus: item.responseStatus,
  }
}

function removeCircularReferences(obj: any) {
  const seen = new WeakSet()
  return JSON.parse(JSON.stringify(obj, (key, value) => {
    if (typeof value === 'object' && value !== null) {
      if (seen.has(value)) return
      seen.add(value)
    }
    return value
  }))
}

function parseJson(value: any): any {
  if (!value) return null
  try {
    return typeof value === 'string' ? JSON.parse(value) : value
  } catch (error) {
    console.error('JSON parse error:', error)
    return value
  }
}

function getTableRowClassName({ row }: { row: any }) {
  return !row.responseStatus.startsWith('2') ? 'warning-row' : ''
}

// Initialization
onMounted(() => {
  fetchLogData(1)
})

// Settings Tab State
const selectedType = ref('all')
const ruleTableData = ref<RuleTableItem[]>([])
const rulePage = ref(1)
const ruleSize = ref(10)
const ruleTotal = ref(0)
const isLoading = ref(false)
const dialogFormVisible = ref(false)
const ruleForm = ref({
  id: 0,
  rule: '',
  type: 'include',
  name: ''
})
const formLabelWidth = '80px'

// Settings Tab Methods
const addInput = () => {
  ruleForm.value = {
    id: 0,
    rule: '',
    type: 'include',
    name: ''
  }
  dialogFormVisible.value = true
}

// 获取规则列表
const getRules = async () => {
  isLoading.value = true
  try {
    const response = await axiosInstance.get('/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos')
    const rules = response.data.items as RuleInfo[]
    
    ruleTableData.value = rules.map(item => ({
      id: item.metadata.name,
      rule: item.spec.rule,
      type: item.spec.isInclude ? 'include' as const : 'exclude' as const,
      creationTimestamp: item.metadata.creationTimestamp,
      name: item.metadata.name
    }))
    
    ruleTotal.value = response.data.total || rules.length
  } catch (error) {
    console.error('获取规则列表失败:', error)
    ElMessage.error('获取规则列表失败')
  } finally {
    isLoading.value = false
  }
}

// 添加状态量
const deleteDialogVisible = ref(false)
const deleteRow = ref<RuleTableItem | null>(null)

// 修改删除处理方法
const handleDelete = (row: RuleTableItem) => {
  deleteRow.value = row
  deleteDialogVisible.value = true
}

// 确认除方法
const confirmDelete = async () => {
  if (!deleteRow.value) return
  
  isLoading.value = true
  try {
    const { status } = await axiosInstance.delete(
      `/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos/${deleteRow.value.name}`
    )

    if (status === 200) {
      deleteDialogVisible.value = false
      ElMessage.success('删除成功')
      
      // 添加延时
      new Promise(resolve => setTimeout(resolve, 100))
      await getRules()
    }
  } catch (error) {
    console.error('删除规则失败:', error)
    ElMessage.error('删除失败')
  } finally {
    isLoading.value = false
  }
}

// 保存规则
const saveRule = async () => {
  if (!ruleForm.value.rule) {
    ElMessage.warning('请输入规则内容')
    return
  }

  isLoading.value = true
  try {
    const isEdit = Boolean(ruleForm.value.id)
    const requestBody = {
      apiVersion: "dailyActive.halo.run/v1alpha1",
      kind: "InterfaceLogRuleInfo",
      metadata: {
        name: isEdit ? ruleForm.value.name : Date.now().toString()
      },
      spec: {
        id: ruleForm.value.id || 0,
        isInclude: ruleForm.value.type === 'include',
        rule: ruleForm.value.rule,
        version: "1"
      }
    }

    const { status } = await axiosInstance[isEdit ? 'put' : 'post'](
      isEdit 
        ? `/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos/${ruleForm.value.name}`
        : '/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos',
      requestBody
    )

    if (status === (isEdit ? 200 : 201)) {
      dialogFormVisible.value = false
      ElMessage.success(isEdit ? '更新成功' : '添加成功')
      await new Promise(resolve => setTimeout(resolve, 100))
      await getRules()
    }
  } catch (error) {
    console.error(ruleForm.value.id ? '更新规则失败:' : '新增规则失败:', error)
    ElMessage.error(ruleForm.value.id ? '更新失败' : '新增失败')
  } finally {
    isLoading.value = false
  }
}

const handleRulePageChange = async (page: number) => {
  // 实现规则分页逻辑
}

// 日志表格的行类名方法
const getLogTableRowClassName = ({ row }: { row: any }) => {
  return row.responseStatus && !row.responseStatus.startsWith('2') ? 'warning-row' : ''
}

// 规则表格的类名方法
const getRuleTableRowClassName = ({ row }: { row: RuleTableItem }) => {
  return row.type === 'include' ? 'include-row' : 'exclude-row'
}

// 新增相关
const addDialogVisible = ref(false)
const newRuleForm = ref({
  rule: '',
  type: 'include' as 'include' | 'exclude'
})

const handleAdd = () => {
  newRuleForm.value = {
    rule: '',
    type: 'include'
  }
  addDialogVisible.value = true
}

const saveNewRule = async () => {
  if (!newRuleForm.value.rule) {
    ElMessage.warning('请输入规则内容')
    return
  }

  isLoading.value = true
  try {
    const requestBody = {
      apiVersion: "dailyActive.halo.run/v1alpha1",
      kind: "InterfaceLogRuleInfo",
      metadata: {
        name: Date.now().toString()
      },
      spec: {
        id: 0,
        isInclude: newRuleForm.value.type === 'include',
        rule: newRuleForm.value.rule,
        version: "1"
      }
    }

    const { status } = await axiosInstance.post(
      '/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos',
      requestBody
    )

    if (status === 201) {
      addDialogVisible.value = false
      ElMessage.success('添加成功')
      await new Promise(resolve => setTimeout(resolve, 100))
      await getRules()
    }
  } catch (error) {
    console.error('新增规���失败:', error)
    ElMessage.error('新增失败')
  } finally {
    isLoading.value = false
  }
}

// 编辑相关
const editDialogVisible = ref(false)
const editRuleForm = ref({
  id: '',
  rule: '',
  type: 'include' as 'include' | 'exclude',
  name: ''
})

const handleEdit = (row: RuleTableItem) => {
  editRuleForm.value = {
    id: row.id,
    rule: row.rule,
    type: row.type,
    name: row.name
  }
  editDialogVisible.value = true
}

const updateRule = async () => {
  if (!editRuleForm.value.rule) {
    ElMessage.warning('请输入规则内容')
    return
  }

  isLoading.value = true
  try {
    const requestBody = {
      apiVersion: "dailyActive.halo.run/v1alpha1",
      kind: "InterfaceLogRuleInfo",
      metadata: {
        name: editRuleForm.value.name
      },
      spec: {
        id: editRuleForm.value.id,
        isInclude: editRuleForm.value.type === 'include',
        rule: editRuleForm.value.rule,
        version: "1"
      }
    }

    const { status } = await axiosInstance.put(
      `/apis/dailyActive.halo.run/v1alpha1/interfaceLogRuleInfos/${editRuleForm.value.name}`,
      requestBody
    )

    if (status === 200) {
      editDialogVisible.value = false
      ElMessage.success('更新成')
      await new Promise(resolve => setTimeout(resolve, 100))
      await getRules()
    }
  } catch (error) {
    console.error('更新规则失败:', error)
    ElMessage.error('更新失败')
  } finally {
    isLoading.value = false
  }
}

// 在模板中根据是否有 id 调用不同的方法
const handleSave = () => {
  if (ruleForm.value.id) {
    updateRule()
  } else {
    saveNewRule()
  }
}
</script>
<template>
  <VPageHeader title="接口日志"></VPageHeader>
  <VCard class="m-0 flex-1 md:m-4">
    <div class="p-3">
      <VTabbar
        v-model:activeId="activeNavId"
        :items="navigationItems"
        :type="'pills'"
        :direction="'row'"
      />
    </div>
    <div v-if="activeNavId==='1'" class="mx-auto max-w-4xl px-4 md:px-8">
      <el-config-provider :value-on-clear="null" :empty-values="[undefined, null]">
        <div class="flex flex-wrap gap-4 items-center">
          <el-select
            v-model="selectedUsers"
            multiple
            filterable
            clearable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="用户名"
            :remote-method="searchUsers"
            :loading="isUserOptionsLoading"
            style="width: 230px"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.value"
              :label="item.value"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="selectedClientIps"
            multiple
            filterable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="请求IP"
            :remote-method="searchClientIps"
            :loading="isClientIpOptionsLoading"
            style="width: 230px"
          >
            <el-option
              v-for="item in clientIpOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="selectedPaths"
            multiple
            filterable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="接口路径"
            :remote-method="searchPaths"
            :loading="isPathOptionsLoading"
            style="width: 230px"
          >
            <el-option
              v-for="item in pathOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="block">
            <el-date-picker
              v-model="searchDateRange"
              type="datetimerange"
              :shortcuts="dateShortcuts"
              range-separator="to"
              start-placeholder="Start date"
              end-placeholder="End date"
            />
          </div>
          <el-button style="color: #2f81f7;--el-button-hover-bg-color: v-bind('blue')" :dark="true"
                     @click="fetchLogData(1)">查询
          </el-button>
        </div>
      </el-config-provider>
      <VCard class="m-0 flex-1 md:m-4">
        <div class="mt-4" style="height: 800px">
          <el-table
            v-loading="isTableLoading"
            :data="logTableData"
            style="width: 100%"
            :row-class-name="getLogTableRowClassName">
            <el-table-column
              prop="accessTime"
              label="请求时间"
              width="240">
            </el-table-column>
            <el-table-column
              prop="username"
              label="户名"
              width="180">
            </el-table-column>
            <el-table-column
              prop="clientIp"
              label="请求IP"
              width="180">
            </el-table-column>
            <el-table-column
              prop="path"
              label="请求URI"
              width="180"
              show-overflow-tooltip>
            </el-table-column>
            <el-table-column
              prop="requestType"
              label="请求类型"
              width="180">
            </el-table-column>
            <el-table-column
              prop="responseStatus"
              label="响应码"
              width="180"
              show-overflow-tooltip>
            </el-table-column>
            <el-table-column align="right">
              <template #default="scope">
                <el-button size="small" @click="handleDetailView(scope.row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </VCard>
      <VPagination v-model:page="currentPage" v-model:size="pageSize" v-model:total="totalItems" @update:page="handlePageChange"/>
    </div>
    <div v-else-if="activeNavId==='2'">
      <VCard class="m-0 flex-1 md:m-4" title="记录接口">
        <div class="mb-4 flex justify-between items-center">
          <el-button 
            type="primary" 
            @click="handleAdd"
            style="--el-button-text-color: #000000; --el-button-bg-color: #409eff; width: 100px;"
          >增加规则</el-button>
          <el-radio-group v-model="selectedType" @change="getRules" size="default">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="include">包含</el-radio-button>
            <el-radio-button label="exclude">排除</el-radio-button>
          </el-radio-group>
        </div>
        <el-table
          v-loading="isLoading"
          :data="ruleTableData"
          :row-class-name="getRuleTableRowClassName"
          style="width: 100%; height: calc(100vh - 300px)"
          :max-height="'calc(100vh - 300px)'">
          <el-table-column
            prop="id"
            label="ID"
            width="180"/>
          <el-table-column
            prop="rule"
            label="规则"
            width="auto"/>
          <el-table-column
            prop="type"
            label="类型"
            width="100">
            <template #default="scope">
              {{ scope.row.type === 'include' ? '包含' : '排除' }}
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="操作" width="140">
            <template #default="scope">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="handleEdit(scope.row)"
                  style="width: 50px; min-width: 50px;"
                >
                  编辑
                </el-button>
                <el-button
                  type="danger"
                  text
                  size="small"
                  style="color: black; width: 50px; min-width: 50px;"
                  @click="handleDelete(scope.row)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-4">
          <VPagination 
            v-model:page="rulePage" 
            v-model:size="ruleSize" 
            v-model:total="ruleTotal" 
            @update:page="handleRulePageChange"
          />
        </div>
        <el-dialog 
          v-model="dialogFormVisible" 
          :title="ruleForm.id ? '编辑规则' : '新增规则'" 
          width="500">
          <el-form :model="ruleForm">
            <el-form-item label="规则内容" :label-width="formLabelWidth">
              <el-input v-model="ruleForm.rule" autocomplete="off" />
            </el-form-item>
            <el-form-item label="规则类型" :label-width="formLabelWidth">
              <el-select v-model="ruleForm.type">
                <el-option label="包括" value="include" />
                <el-option label="不包括" value="exclude" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="dialogFormVisible = false">取消</el-button>
              <el-button type="primary" @click="handleSave" style="--el-button-text-color: #000000">确定</el-button>
            </div>
          </template>
        </el-dialog>
      </VCard>
    </div>
  </VCard>
  <el-dialog
    v-model="detailDialogVisible"
    title="接口详情"
    width="80%">
    <div v-if="currentDetail" style="max-height: 70vh; overflow-y: auto;">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="请求时间">{{ currentDetail.accessTime }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="请求IP">{{ currentDetail.clientIp }}</el-descriptions-item>
        <el-descriptions-item label="请求URI" class="break-all">{{ currentDetail.path }}</el-descriptions-item>
        <el-descriptions-item label="请求类型" style="width: 180px">{{ currentDetail.requestType }}</el-descriptions-item>
        <el-descriptions-item label="响应码">{{ currentDetail.responseStatus }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <JsonViewer
            v-if="currentDetail.requestParams"
            :value="parseJson(currentDetail.requestParams)"
            :expand-depth="2"
            copyable
            sort
          />
        </el-descriptions-item>
        <el-descriptions-item label="请求头">
          <JsonViewer
            v-if="currentDetail.requestHeader"
            :value="parseJson(currentDetail.requestHeader)"
            :expand-depth="2"
            copyable
            sort
          />
        </el-descriptions-item>
        <el-descriptions-item label="请求体">
          <JsonViewer
            v-if="currentDetail.requestBody"
            :value="parseJson(currentDetail.requestBody)"
            :expand-depth="2"
            copyable
            sort
          />
        </el-descriptions-item>
        <el-descriptions-item label="响应头">
          <JsonViewer
            v-if="currentDetail.responseHeader"
            :value="parseJson(currentDetail.responseHeader)"
            :expand-depth="2"
            copyable
            sort
          />
        </el-descriptions-item>
        <el-descriptions-item label="响应内容">
          <JsonViewer
            v-if="currentDetail.responseBody"
            :value="parseJson(currentDetail.responseBody)"
            :expand-depth="2"
            copyable
            sort
          />
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>
  <!-- 删除确认对话框 -->
  <el-dialog
    v-model="deleteDialogVisible"
    title="删除确认"
    width="420px"
    center
    :close-on-click-modal="false"
  >
    <el-descriptions :column="1" border>
      <el-descriptions-item 
        label="规则内容" 
        label-class-name="rule-label"
        class="rule-content"
      >
        {{ deleteRow?.rule }}
      </el-descriptions-item>
      <el-descriptions-item 
        label="规则类型"
        label-class-name="rule-label"
      >
        {{ deleteRow?.type === 'include' ? '包含' : '排除' }}
      </el-descriptions-item>
    </el-descriptions>
    <div class="delete-confirm-text">是否确认删除该规则？</div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmDelete" 
          :loading="isLoading"
          style="color: black"
        >
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
  <!-- 新增规则对话框 -->
  <el-dialog 
    v-model="addDialogVisible" 
    title="新增规则" 
    width="500"
  >
    <el-form :model="newRuleForm">
      <el-form-item label="规则内容" :label-width="formLabelWidth">
        <el-input v-model="newRuleForm.rule" autocomplete="off" />
      </el-form-item>
      <el-form-item label="规则类型" :label-width="formLabelWidth">
        <el-select v-model="newRuleForm.type">
          <el-option label="包括" value="include" />
          <el-option label="不包括" value="exclude" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="saveNewRule" 
          :loading="isLoading"
          style="--el-button-text-color: #000000"
        >确定</el-button>
      </div>
    </template>
  </el-dialog>
  <!-- 编辑规则对话框 -->
  <el-dialog 
    v-model="editDialogVisible" 
    title="编辑规则" 
    width="500"
  >
    <el-form :model="editRuleForm">
      <el-form-item label="规则内容" :label-width="formLabelWidth">
        <el-input v-model="editRuleForm.rule" autocomplete="off" />
      </el-form-item>
      <el-form-item label="规则类型" :label-width="formLabelWidth">
        <el-select v-model="editRuleForm.type">
          <el-option label="包括" value="include" />
          <el-option label="不包括" value="exclude" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="updateRule" 
          :loading="isLoading"
          style="--el-button-text-color: #000000"
        >确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<style lang="scss">
.date-picker {
  & input {
    border-radius: 0.375rem;
  }
}

.el-table .warning-row {
  background-color: oldlace;
}

.el-table .success-row {
  background-color: #f0f9eb;
}

.jv-container {
  max-height: inherit;
  overflow: auto;
  
  .jv-node {
    &.toggle {
      position: relative;
      padding-left: 15px;
      margin-left: -15px;
      
      &:before {
        position: absolute;
        left: 0;
      }
    }
  }
  
  .jv-code {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.jv-container.jv-light {
  background: none;
}

.el-descriptions__cell {
  &.is-label-top {
    min-width: 180px;
    width: 180px;
  }
}

.el-descriptions-item__content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: calc(100% - 180px);
}

.jv-container {
  overflow: auto;
  max-height: 400px;
  border: 1px solid #ebeef5;
  padding: 10px;
  
  .jv-code {
    white-space: nowrap !important;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.el-descriptions-item__content {
  overflow-x: auto;
}

.el-table {
  .include-row {
    background-color: #f0f9eb;  // 绿色背景
    &:hover > td {
      background-color: #e1f3d8 !important;
    }
  }
  
  .exclude-row {
    background-color: #fef0f0;  // 红色背景
    &:hover > td {
      background-color: #fde2e2 !important;
    }
  }
}

.delete-confirm-dialog {
  width: 420px;
}

.delete-confirm-dialog .el-message-box__header {
  padding: 15px;
  padding-bottom: 10px;
}

.delete-confirm-dialog .el-message-box__content {
  padding: 20px;
  padding-top: 10px;
}

.delete-confirm-dialog .el-message-box__btns {
  padding: 10px 15px 15px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.delete-confirm-text {
  margin-top: 16px;
  text-align: center;
  color: #606266;
}

.el-descriptions {
  margin-top: 8px;
}

.rule-label {
  width: 100px;
  text-align: right;
  padding-right: 12px;
}

.rule-content .el-descriptions-item__content {
  word-break: break-all;
}

.el-descriptions__cell {
  padding: 12px !important;
}

.el-button {
  &.el-button--small {
    height: 24px;
    line-height: 24px;
  }
}
html,body {
  padding-right: 0 !important;
}
</style>
