import { definePlugin } from "@halo-dev/console-shared";
import HomeView from "./views/HomeView.vue";
import { IconPlug } from "@halo-dev/components";
import { markRaw } from "vue";
import {ElTable} from "element-plus";

export default definePlugin({
  components: {
    ElTable
  },
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/interfaceLog",
        name: "interfaceLog",
        component: HomeView,
        meta: {
          title: "接口日志",
          searchable: true,
          menu: {
            name: "接口日志",
            group: "system",
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
