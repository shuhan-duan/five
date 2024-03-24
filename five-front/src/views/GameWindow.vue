<script setup>
import router from '@/router'

</script>

<template>
    <div>
        <!-- Buttons -->
        <div style="display: flex; justify-content: space-between; padding: 0 60px;">
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
    height: 180px;
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
