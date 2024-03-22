import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUserIderStore = defineStore('userIder', () => {
  // State: userInfo to hold user data
  const userInfo = ref({
    id: -1,
    username: '',
    jwt: '',
  });

  // Action: Updates userInfo with new data
  const setUserInfo = (data) => {
    userInfo.value = { ...userInfo.value, ...data };
  };

  // State: loginForm to store login credentials temporarily
  const loginForm = ref({});

  // Action: Stores login credentials
  const setLoginForm = (data) => {
    loginForm.value = { ...loginForm.value, ...data };
  };

  return {
    // Exposed state and actions
    userInfo,
    setUserInfo,
    loginForm,
    setLoginForm,
  };
}, {
  persist: true, // Enables persistence with Pinia's built-in plugin
});
