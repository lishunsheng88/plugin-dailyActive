<script lang="ts" setup>
import {
  VCard, VLoading,
  VPageHeader, VPagination, VTabbar,
} from "@halo-dev/components";
import {axiosInstance} from "@halo-dev/api-client"
import {onMounted, ref} from 'vue'
import {Search} from '@element-plus/icons-vue'
import {rowProps} from "element-plus";

const tableData = ref<any>([])
const page = ref(1);
const size = ref(10);
const total = ref(0);
const isLoading = ref(true)
const items = [
  {id: "1", label: "接口日志", icon: "icon-log"},
  {id: "2", label: "基本设置", icon: "icon-log"},
]
const activeId = ref("1")

init()
isLoading.value = false

interface ListItem {
  value: string
  label: string
}

const optionsUsers = ref<ListItem[]>([])
const valueUsers = ref<string[]>([])
const loadingUsers = ref(false)
const remoteMethodUsers = async (query: string) => {
  if (query) {
    loadingUsers.value = true;
    try {
      const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/users?username=` + query);
      optionsUsers.value = response.data;
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      loadingUsers.value = false;
    }
  } else {
    optionsUsers.value = [];
  }
}
const optionsClientIps = ref<ListItem[]>([])
const valueClientIps = ref<string[]>([])
const loadingClientIps = ref(false)
const remoteMethodClientIps = async (query: string) => {
  if (query) {
    loadingClientIps.value = true;
    try {
      const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/clientIps?clientIp=` + query);
      optionsClientIps.value = response.data;
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      loadingClientIps.value = false;
    }
  } else {
    optionsClientIps.value = [];
  }
}
const optionsPaths = ref<ListItem[]>([])
const valuePaths = ref<string[]>([])
const loadingPaths = ref(false)
const remoteMethodPaths = async (query: string) => {
  if (query) {
    loadingPaths.value = true;
    try {
      const response = await axiosInstance.get(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/paths?path=` + query);
      optionsPaths.value = response.data;
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      loadingPaths.value = false;
    }
  } else {
    optionsPaths.value = [];
  }
}


async function handlePageChange(val: number) {
  isLoading.value = true;
  await updateData(val);
  isLoading.value = false;
}

function removeCircularReferences(obj: any) {
  const seen = new WeakSet();
  return JSON.parse(JSON.stringify(obj, (key, value) => {
    if (typeof value === 'object' && value !== null) {
      if (seen.has(value)) {
        return;
      }
      seen.add(value);
    }
    return value;
  }));
}

async function updateData(pageNum: number) {
  const requestBody = {
    page: pageNum,
    size: size.value,
    username: valueUsers.value.map(user => user.substring(1)),
    clientIp: valueClientIps.value,
    path: valuePaths.value,
    accessTimes: dateTime.value === '' ? [] : dateTime.value,
  }
  tableData.value = [];
  await axiosInstance.post(`/apis/dailyActive.halo.run/v1alpha1/interfaceLog/search`, removeCircularReferences(requestBody))
    .then(response => {
      const logInfoData = response.data
      page.value = logInfoData.page
      size.value = logInfoData.size
      total.value = logInfoData.total
      for (let i = 0; i < logInfoData.items.length; i++) {
        total.value = logInfoData.total
        tableData.value.push({
          id: logInfoData.items[i].id,
          accessTime: logInfoData.items[i].accessTime,
          username: logInfoData.items[i].username,
          clientIp: logInfoData.items[i].clientIp,
          path: logInfoData.items[i].path,
          requestType: logInfoData.items[i].requestType,
          responseStatus: logInfoData.items[i].responseStatus,
        })
      }
    })
}

function init() {
  const requestBody = {
    page: 1,
    size: 10,
  }
  axiosInstance.post("/apis/dailyActive.halo.run/v1alpha1/interfaceLog/search", requestBody)
    .then(response => {
      const logInfoData = response.data
      for (let i = 0; i < logInfoData.items.length; i++) {
        total.value = logInfoData.total
        tableData.value.push({
          id: logInfoData.items[i].id,
          accessTime: logInfoData.items[i].accessTime,
          username: logInfoData.items[i].username,
          clientIp: logInfoData.items[i].clientIp,
          path: logInfoData.items[i].path,
          requestType: logInfoData.items[i].requestType,
          responseStatus: logInfoData.items[i].responseStatus,
        })
      }
    })
}

const shortcuts = [
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

const dateTime = ref('')

function search() {
  console.log('search')
}

const tableRowClassName = ({row}: {
  row: any
}) => {
  if (!row.responseStatus.startsWith('2')) {
    return 'warning-row'
  }
  return ''
}

function detail() {
  console.error('detail')
}

const inputs = ref<any>([]);

function addInput() {
  inputs.value.push({value: ''});
}

const dialogTableVisible = ref(false)
const dialogFormVisible = ref(false)
const formLabelWidth = '140px'

const form = ref({
  name: '',
  region: '',
})
</script>
<template>
  <VPageHeader title="接口日志"></VPageHeader>
  <VCard class="m-0 flex-1 md:m-4">
    <div class="p-3">
      <VTabbar
        v-model:activeId="activeId"
        :items="items"
        :type="'pills'"
        :direction="'row'"
      />
    </div>
    <div v-if="activeId==='1'" class="mx-auto max-w-4xl px-4 md:px-8">
      <el-config-provider :value-on-clear="null" :empty-values="[undefined, null]">
        <div class="flex flex-wrap gap-4 items-center">
          <el-select
            v-model="valueUsers"
            multiple
            filterable
            clearable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="用户名"
            :remote-method="remoteMethodUsers"
            :loading="loadingUsers"
            style="width: 230px"
          >
            <el-option
              v-for="item in optionsUsers"
              :key="item.value"
              :label="item.value"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="valueClientIps"
            multiple
            filterable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="请求IP"
            :remote-method="remoteMethodClientIps"
            :loading="loadingClientIps"
            style="width: 230px"
          >
            <el-option
              v-for="item in optionsClientIps"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="valuePaths"
            multiple
            filterable
            remote
            collapse-tags
            reserve-keyword
            remote-show-suffix
            collapse-tags-tooltip
            placeholder="接口路径"
            :remote-method="remoteMethodPaths"
            :loading="loadingPaths"
            style="width: 230px"
          >
            <el-option
              v-for="item in optionsPaths"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="block">
            <el-date-picker
              v-model="dateTime"
              type="datetimerange"
              :shortcuts="shortcuts"
              range-separator="to"
              start-placeholder="Start date"
              end-placeholder="End date"
            />
          </div>
          <el-button style="color: #2f81f7;--el-button-hover-bg-color: v-bind('blue')" :dark="true"
                     @click="updateData(1)">查询
          </el-button>
        </div>
      </el-config-provider>
      <VCard class="m-0 flex-1 md:m-4">
        <div class="mt-4" style="height: 800px">
          <el-table
            v-loading="isLoading"
            :data="tableData"
            style="width: 100%"
            :row-class-name="tableRowClassName">
            <el-table-column
              prop="accessTime"
              label="请求时间"
              width="240">
            </el-table-column>
            <el-table-column
              prop="username"
              label="用户名"
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
            <el-table-column>
              <el-button size="small" @click="detail()">
                详情
              </el-button>
            </el-table-column>
          </el-table>
        </div>
      </VCard>
      <VPagination v-model:page="page" v-model:size="size" v-model:total="total" @update:page="handlePageChange"/>
    </div>
    <div v-else-if="activeId==='2'">
      <VCard class="m-0 flex-1 md:m-4" title="记录接口">
        <VCard class="m-0 flex-1 md:m-4" title="包括">
          <div>
            <el-button @click="addInput">增加规则</el-button>
            <el-table
              v-loading="isLoading"
              :data="tableData"
              style="width: 100%"
              :row-class-name="tableRowClassName">
              <el-table-column
                prop="accessTime"
                label="id"
                width="240"/>
              <el-table-column
                prop="accessTime"
                label="规则"
                width="240"/>
              <el-table-column>
                <el-button size="small" @click="dialogFormVisible = true;">
                  编辑
                </el-button>
                <el-button size="small" @click="detail()">
                  删除
                </el-button>
              </el-table-column>
            </el-table>
            <el-dialog v-model="dialogFormVisible" title="Shipping address" width="500">
              <el-form :model="form">
                <el-form-item label="Promotion name" :label-width="formLabelWidth">
                  <el-input v-model="form.name" autocomplete="off" />
                </el-form-item>
                <el-form-item label="Zones" :label-width="formLabelWidth">
                  <el-select v-model="form.region" placeholder="Please select a zone">
                    <el-option label="Zone No.1" value="shanghai" />
                    <el-option label="Zone No.2" value="beijing" />
                  </el-select>
                </el-form-item>
              </el-form>
              <template #footer>
                <div class="dialog-footer">
                  <el-button @click="dialogFormVisible = false">Cancel</el-button>
                  <el-button type="primary" @click="dialogFormVisible = false" style="color: #2f81f7;--el-button-hover-bg-color: v-bind('blue')" :dark="true">
                    Confirm
                  </el-button>
                </div>
              </template>
            </el-dialog>
          </div>
        </VCard>
        <VCard class="m-0 flex-1 md:m-4" title="不包括">
          <div>
            <el-button @click="addInput">增加规则</el-button>
          </div>
        </VCard>
      </VCard>
    </div>
  </VCard>
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
</style>
