<script setup>
import { ref } from 'vue'
import userInfoShow from '@/components/UserInfoShow.vue'
import { getUserInfo } from '@/api/account.js'
import { useUserIderStore } from '@/stores/userIder'

const userIderStore = useUserIderStore()
const userInfos = ref({}) // User information
const drawer = ref(false) // Controls the visibility of the user info drawer

// Function to initialize and get user information
const init = async () => {
    try {
        const response = await getUserInfo(userIderStore.userInfo.id)
        userInfos.value = response.data
        const jsonUserInfos = JSON.stringify(userInfos.value)
        console.log(jsonUserInfos)
    } catch (error) {
        console.error("Failed to fetch user info:", error)
    }
}

init()
</script>

<template>
    <div class="background">
        <el-image src="/img/63cfb52c485521674556716738.jpg" />
    </div>
    <!-- Menu -->
    <el-menu class="menu" mode="horizontal" :unique-opened="true" :ellipsis="false" :router="true">
        <div style="flex-grow: 1;"></div>

        <!-- Personal information button -->
        <el-menu-item>
            <span style="padding: 5px; border-radius: 5px;">
                <el-button link @click="drawer = true">
                    <span class="myButton">
                        <div>
                        <span>profil</span>
                        </div>
                        <div>
                        <span>{{ userInfos.username}}</span>
                        </div>
                    </span>
                </el-button>
            </span>
            <el-drawer v-model="drawer" :with-header="false" :show-close="false" :size="300">
                <userInfoShow :userInfo="userInfos" @initMainWindow="init" />
            </el-drawer>
        </el-menu-item>
    </el-menu>

    <!-- Main window -->
    <div class="mainWindow">
        <router-view />
    </div>
</template>

<style scoped>
.background {
    position: fixed;
    width: 100%;
    height: 100%;
    z-index: 0;
}

.mainWindow {
    position: fixed;
    left: 30px;
    top: 70px;
    display: inline-block;
    box-shadow: var(--el-box-shadow-dark);
    padding: 10px;
    z-index: 1;
    background-color: transparent;
    border-radius: 50px;
    height: 580px;
}

.menu {
    position: absolute;
    width: 100%;
    top: 0;
}

.menu.menu-item {
    display: inline-block;
}

body {
    height: 89%;
}

</style>
