<script setup>
import { ref ,defineProps } from 'vue'
import { out } from '@/api/account.js'
import { useUserIderStore } from '@/stores/userIder'
import router from '@/router'

const props = defineProps({
  userInfo: Object
})

const userIderStore = useUserIderStore()

const deleteDialogVisible = ref(false)
const onDelete = async () => { // delete account
  await out(userInfo.id)
  // delete successfully
  deleteDialogVisible.value = false
  localStorage.removeItem(userIderStore.$id) // Remove locally stored information
  router.push('/introduction')
}

const logoutDialogVisible = ref(false)
const onLogout = () => { // Se déconnecter
  logoutDialogVisible.value = false
  // Remove locally stored information
  localStorage.removeItem(userIderStore.$id)
  router.push('/introduction')
}

</script>

<template>
  <div style="display: inline-block; min-width: 200px; max-width: 300px;">
    <!-- Top information -->
    <div class="topShow">
      <div style="font-size: 50px;">{{ userInfo.username }}</div>
    </div>

    <!-- Bottom information -->
    <el-card>
      <div class="count">
        <div>
          <span>Jeux totaux</span><span>{{ userInfo.gameTotalCounts }}</span>
        </div>
        <div>
          <span>Jeux entre amis</span><span>{{ userInfo.gamePersonCounts }}</span>
        </div>
        <div>
          <span>Jeux contre l'IA</span><span>{{ userInfo.gameAiCounts }}</span>
        </div>
        <div>
          <span>Victoires</span><span>{{ userInfo.gameSuccessCounts}}</span>
        </div>
        <div>
          <span>Défaites</span><span>{{ userInfo.gameFailCounts }}</span>
        </div>
        <div>
          <span>Nuls</span><span>{{ userInfo.gameDeadHeatCounts }}</span>
        </div>
      </div>
    </el-card>


    <div v-show="true" style="text-align: right;">

      <!-- Se déconnecter button -->
      <el-button style="margin-right: 10px;" link @click="logoutDialogVisible = true">Se déconnecter</el-button>
      <el-dialog v-model="logoutDialogVisible" title="Se déconnecter" width="30%" center>
        <div style="text-align: center; font-size: 25px;">Êtes-vous sûr de vouloir vous déconnecter?</div>
        <template #footer>
          <span class="dialog-footer">
            <el-button size="large" type="primary" @click="logoutDialogVisible = false">Annuler</el-button>
            <el-button size="large" @click="onLogout">Confirmer</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- Account delete button -->
      <el-button link @click="deleteDialogVisible = true">Supprime compte</el-button>
      <el-dialog v-model="deleteDialogVisible" title="Supprime compte" width="30%" center>
        <div style="text-align: center; font-size: 25px;">Êtes-vous sûr de vouloir supprimer votre compte ?</div>
        <template #footer>
          <span class="dialog-footer">
            <el-button size="large" type="primary" @click="deleteDialogVisible = false">Annuler</el-button>
            <el-button size="large" @click="onDelete">Confirmer</el-button>
          </span>
        </template>
      </el-dialog>
    </div>

  </div>
</template>

<style scoped>

.topShow {
  background-color: #2c3e50;
  color: white;
  padding-bottom: 30px;
  line-height: 30px;
  padding: 5px;
}

.topShow>div {
  text-align: center;
}

.count {
  line-height: 50px;
}

.count>div {
  display: flex;
  justify-content: space-between;
}
</style>
