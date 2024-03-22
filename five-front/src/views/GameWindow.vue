<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'


// AI battle difficulty selection
const selectDifficultyDialogVisible = ref(false)
const difficultyTitle = ref('Veuillez choisir une difficulté')
const difficulty = ref(-1)
const imgUrl = ref('/img/thinking.png')

// Watch for difficulty changes and update title and image accordingly
watch(difficulty, (newValue) => {
    if (newValue === 1) {
        difficultyTitle.value = 'Facile : Juste pour s\'échauffer'
        imgUrl.value = '/img/easy.png'
    } else if (newValue === 2) {
        difficultyTitle.value = 'Normal : Un défi équilibré'
        imgUrl.value = '/img/common.png'
    } else if (newValue === 3) {
        difficultyTitle.value = 'Difficile : Un vrai challenge'
        imgUrl.value = '/img/devil.png'
    }
})

// Confirm difficulty and start the AI battle
const confirmDifficulty = () => {
    if (difficulty.value === -1) {
        ElMessage.error('Veuillez choisir une difficulté')
    } else {
        selectDifficultyDialogVisible.value = false
        router.push({ path: '/boardAI', query: { difficulty: difficulty.value } })
    }
}
</script>

<template>
    <div>
        <!-- Buttons -->
        <div style="display: flex; justify-content: space-between; padding: 0 60px;">
            <!-- Button for AI battle -->
            <el-button class="button" round @click="selectDifficultyDialogVisible = true">
                <div style="font-size: 20px;">Bataille Ordinateur</div>
            </el-button>

            <!-- Difficulty selection dialog -->
            <el-dialog ref="difficultyDialog" v-model="selectDifficultyDialogVisible" width="30%" center>
                <template #title>
                    <span style="font-size: 30px;">Bataille Ordinateur</span>
                </template>

                <span style="text-align: center;">
                    <div style="font-size: 30px;">{{ difficultyTitle }}</div>
                    <div style="display: flex; justify-content: space-around; padding: 30px 50px;">
                        <el-button style="width: 150px;" size="large" type="primary" @click="difficulty = 1">Facile</el-button>
                        <el-button style="width: 150px;" size="large" type="warning" @click="difficulty = 2">Normal</el-button>
                        <el-button style="width: 150px;" size="large" type="danger" @click="difficulty = 3">Difficile</el-button>
                    </div>
                    <div style="text-align: center; margin-right: 10px;">
                        <el-image style="width: 100px; height: 100px" :src="imgUrl" />
                    </div>

                </span>
                <template #footer>
                    <span>
                        <el-button size="large" @click="selectDifficultyDialogVisible = false">Annuler</el-button>
                        <el-button type="primary" size="large" @click="confirmDifficulty">Confirmer</el-button>
                    </span>
                </template>
            </el-dialog>

            <!-- Button for online battle with friends -->
            <el-button class="button" round @click="router.push('/boardFriend')">
                <div style="font-size: 20px;">Bataille entre amis</div>
            </el-button>
        </div>

    </div>
</template>

<style scoped>

.verification-container {
    display: flex;
}

.verification-input {
    width: 54px;
    height: 54px;
    margin-right: 30px;
    text-align: center;
    font-size: 20px;
    border: 1px solid #ebebeb;
    border-radius: 5px;
}

.verification-input>.last-child {
    margin-right: 0;
}

.verification-input:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 5px #007bff;
}

div {
    display: flex;
    justify-content: center; 
    align-items: center; 
    flex-direction: column; 
    gap: 20px;
    padding: 20px; 
}

.button {
    width: 250px;
    height: 250px;
    background-color: transparent !important;
    border-color: transparent !important;
}

.button-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px; 
}

.myItem {
    display: flex;
    justify-content: space-around;
    padding: 5px;
    border-radius: 5px;
}

.myItem>span {
    line-height: 40px;
    margin-top: 10px;
    margin-right: 20px;
}

.item {
    display: flex;
    justify-content: space-around;
    padding: 5px;
    border-radius: 5px;
}


.item:hover {
    background-color: rgb(223, 220, 220);
}

.item>span {
    line-height: 40px;
    margin-top: 10px;
    margin-right: 20px;
}
</style>
