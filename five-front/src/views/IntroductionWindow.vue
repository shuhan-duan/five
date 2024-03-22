<script setup>
// Introduction page, homepage
import { ref } from 'vue'
import { login, register } from '@/api/account.js'
import { useUserIderStore } from '@/stores/userIder'
import router from '@/router'
import { ElMessage } from 'element-plus'

// State management
const loginDialogVisible = ref(false)
const userIderStore = useUserIderStore()
const status = ref(true) // true for login, false for register
const usernameInp = ref(null)
const passwordInp = ref(null)
const form = ref({
  username: '',
  password: ''
})

// Methods
const openDialog = () => {
  loginDialogVisible.value = true
  form.value = { username: '', password: '' }
}

const validateForm = () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.error('Le nom d’utilisateur et le mot de passe ne peuvent pas être vides')
    return false
  }
  return true
}

const onSubmit = async () => {
  if (!validateForm()) return

  try {
    const response = status.value ? await login(form.value) : await register(form.value)
    ElMessage.success(`Connexion réussie`)
    userIderStore.setUserInfo(response.data)
    loginDialogVisible.value = false
    router.push('/main')
  } catch (error) {
    ElMessage.error(`Échec de la ${status.value ? 'connexion' : 'inscription'}: ${error.message}`)
  }
}

const navigateInput = (event, direction) => {
  if (event.key === 'Enter' && direction === 'down') passwordInp.value.focus()
  if (event.key === 'ArrowUp' && direction === 'up') usernameInp.value.focus()
  if (event.key === 'Enter' && direction === 'submit') onSubmit()
}
</script>

<template>
  <!-- Menu -->
  <el-menu
    style="width: 100%; font-family: Arial;"
    class="menu"
    mode="horizontal"
    :unique-opened="true"
    :ellipsis="false"
  >
    <div style="flex-grow: 1"></div>

    <!-- Login/personal info button -->
    <el-menu-item>
      <!-- Login button -->
      <span>
        <el-button style="margin-bottom: 15px" size="large" link @click="openDialog">
          Connexion
        </el-button>
        <!-- Login dialog -->
        <el-dialog v-model="loginDialogVisible" width="30%">
          <div style="max-width: 600px">
            <h1 style="text-align: center">{{ status ? 'Connexion' : 'Inscription' }}</h1>
            <el-form :model="form" label-width="120px">
              <el-form-item label="Nom d'utilisateur">
                <el-input
                  ref="usernameInp"
                  @keydown="e => navigateInput(e, 'down')"
                  v-model="form.username"
                  :placeholder="status ? 'Entrez votre nom d’utilisateur' : 'Définissez un nouveau nom d’utilisateur'"
                />
              </el-form-item>

              <el-form-item label="Mot de passe">
                <el-input
                  ref="passwordInp"
                  @keydown="e => navigateInput(e, 'submit')"
                  v-model="form.password"
                  type="password"
                  :placeholder="status ? 'Entrez votre mot de passe' : 'Définissez un nouveau mot de passe'"
                  show-password
                />
              </el-form-item>
              <div>
                <el-button
                  style="float: right"
                  size="small"
                  type="primary"
                  @click="status = !status"
                >
                  {{ status ? 'Inscription' : 'Connexion' }}
                </el-button>
              </div>

              <el-form-item>
                <el-button type="primary" @click="onSubmit">{{ status ? 'Connexion' : 'Inscription' }}</el-button>
                <el-button @click="loginDialogVisible = false">Annuler</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-dialog>
      </span>
    </el-menu-item>
  </el-menu>

  <div class="introduction" style="display: flex; justify-content: flex-end; align-items: center; height: 100vh;">
    <div style="font-size: 80px; text-align: center; color:black;">
      <p>Prêt pour Five In A Row ?</p>
    </div>
  </div>
</template>

<style scoped>
.introduction {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100vw;
  height: 100vh;
  background-image: url(/img/introduction2.jpg);
  background-size: cover;
  background-position: center;
}

.menu {
  position: absolute;
  width: 100%;
  top: 0;
}
</style>
