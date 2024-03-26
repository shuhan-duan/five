<script setup>

import { ref, reactive, watch, nextTick,computed } from 'vue'
import { useUserIderStore } from '@/stores/userIder.js'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getUserInfo } from '@/api/account'
import { baseURL } from '@/utils/request'

const boardStates = reactive(
  Array(15)
    .fill()
    .map(() => Array(15).fill(0))
)

const stepOrder = ref(1) // Current step number
const gameId = ref(-1) // Game ID, obtained from the backend
const currentPlayer = ref(0) // 1 represents black chess, 2 represents white chess , 0 represents spectator
const userIderStore = useUserIderStore()
const id = userIderStore.userInfo.id // My ID
const myInfo = ref({}) // My personal information
const blackUrl = ref('/assets/pieces/black.png')
const whiteUrl = ref('/assets/pieces/white.png')
const roomId = ref('') // Room number
const verificationCodes = ref(['', '', '', '', '']) // Room number input box
const friendDialogVisible = ref(true) // Control the dialog for entering the room number
const lookerDialogVisible = ref(false) // Control the dialog for choosing whether to watch
const playerType = ref('') // Current user type: spectator, black chess player, or white chess player
const fullscreenLoading = ref(false) // Control the loading interface display
const chatMessage = ref('') // Chat message
const playSocket = ref({}) // WebSocket
const isWait = ref(false) // Whether waiting for the opponent to move
const actors = ref([])
const chatMessageInp = ref(false)
const isGameing = ref(false) // Whether the game is in progress

// timer
const timer = ref(0)
let intervalId = null
const timerRunning = ref(false)
function startTimer() {
  if (!timerRunning.value) {
    intervalId = setInterval(() => {
      timer.value++
    }, 1000)
    timerRunning.value = true
  }
}
function stopTimer() {
  if (intervalId) {
    clearInterval(intervalId)
    intervalId = null
    timerRunning.value = false
  }
}
const formatTime = computed(() => {
  const minutes = Math.floor(timer.value / 60)
  const seconds = timer.value % 60
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
})

// Game over message translation
const gameOverToCN = (num) => {
  if (num === 1) {
    return 'Noir gagne'
  } else if (num === 2) {
    return 'Blanc gagne'
  } else {
    return 'Égalité'
  }
}

// Room code input handlers
const handleInput = (index, event) => {
  const value = event.target.value
  verificationCodes.value[index] = value

  if (value && index < verificationCodes.value.length - 1) {
    const nextInput = event.target.nextElementSibling
    if (nextInput) {
      nextTick(() => {
        nextInput.focus()
      })
    }
  }
}
const handleKeyDown = (index, event) => {
  if (event.key === 'Backspace' && !event.target.value && index > 0) {
    const prevInput = event.target.previousElementSibling
    if (prevInput) {
      nextTick(() => {
        prevInput.focus()
      })
    }
  }
}

const MessageType = {
  Warning: 0,
  UserActorConfirm: 2,
  Chat: 3,
  Move: 4,
  RoomCountChange: 5,
  ObserverUpdate: 6,
};

const onGameWithFriend = async () => {
  if (verificationCodes.value.join('').length < 5) {
    ElMessage.error("Veuillez entrer un numéro de salle valide");
    return;
  }
  await loadAndSetUserInfo();
  const roomId = verificationCodes.value.join('');
  initializeWebSocket(roomId);
  friendDialogVisible.value = false;
};

const loadAndSetUserInfo = async () => {
  const response = await getUserInfo(id);
  myInfo.value = response.data;
};

const initializeWebSocket = (roomId) => {
  const url = new URL(baseURL)
  const hostAndPort = `${url.hostname}:${url.port}`
  const wsUrl = `ws://${hostAndPort}/game/online/five/${roomId}/${id}`;
  console.log("wsUrl", wsUrl);
  playSocket.value = new WebSocket(wsUrl);
  
  playSocket.value.onopen = () => console.log("WebSocket connection established");
  playSocket.value.onclose = handleSocketClose;
  playSocket.value.onerror = (error) => console.error("WebSocket error:", error);
  playSocket.value.onmessage = handleSocketMessage;
  fullscreenLoading.value = true
};

const handleSocketClose = () => {
  isGameing.value = false 
  chatMessageInp.value = true 
  stopTimer()
  router.push('/main/game')
};

const handleSocketMessage = (event) => {
  const socketmessage = JSON.parse(event.data);
  handleMessage(socketmessage);
};

const handleMessage = (socketmessage) => {
  switch (socketmessage.type) {
    case MessageType.Warning:
      handleWarningMessage(socketmessage);
      break;
    case MessageType.UserActorConfirm:
      handleUserActorConfirmMessage(socketmessage);
      break;
    case MessageType.Chat:
      handleChatMessage(socketmessage);
      break;
    case MessageType.Move:
      handleMoveMessage(socketmessage);
      break;
    case MessageType.RoomCountChange:
      handleRoomCountChangeMessage(socketmessage);
      break;
    case MessageType.ObserverUpdate:
      handleObserverUpdateMessage(socketmessage);
      break;
    default:
      console.warn(`Unhandled message type: ${socketmessage.type}`);
  }
};

const handleWarningMessage = (message) => {
  fullscreenLoading.value = false // Close the loading window
  lookerDialogVisible.value = true // Open the observer selection window
};

const handleUserActorConfirmMessage = (message) => {
  isGameing.value = true;
  playerType.value = message.role;
  gameId.value = message.gameId;
  adjustGameStartState();
};

const adjustGameStartState = () => {
  if (playerType.value === 'Joueur Blanc') {
    isWait.value = true;
    currentPlayer.value = 2;
  } else {
    isWait.value = false;
    currentPlayer.value = 1;
  }
  //initialization 
  fullscreenLoading.value = false
  addLogList({
    name: 'Système',
    message: '-Le combat commence-'
  })
  startTimer()
};

const handleChatMessage = (socketmessage) => {
  if (myInfo.value.id != socketmessage.id) {
    if (socketmessage.role == 'Joueur Noir') {
      showBlackerMessage(socketmessage.message)
    } else if (socketmessage.role == 'Joueur Blanc') {
      showWhiterMessage(socketmessage.message)
    } else {
      addLogList({
        name: actors.value.filter((item) => item.id == socketmessage.id)[0].username,
        message: socketmessage.message
      })
    }
  }
};

const handleGameOver = (message) => {
  const winnerRole = message.role;
  const winnerMessage = `Le jeu est terminé, ${winnerRole} gagne et votre salle sera déconnectée`;
  ElMessage.success(winnerMessage);
  
  const winnerName = actors.value.filter((item) => item.role == winnerRole)[0].username;
  addLogList({ name: winnerName, message: gameOverToCN(message.isGameOver) });
  router.push('/main/game')
};

const handleMoveMessage = (socketmessage) => {
  let [x, y] = parseChessMove(socketmessage.message);
  
  if (playerType.value !== 'Spectateur') {
    if (socketmessage.isGameOver == 0) {
      if (socketmessage.role !== playerType.value) {
        boardStates[x][y] = currentPlayer.value % 2 + 1;
        isWait.value = false;
      }
    } else {
      handleGameOver(socketmessage);
    }
  } else {
    boardStates[x][y] = currentPlayer.value;
    currentPlayer.value = currentPlayer.value % 2 + 1;
    if (socketmessage.isGameOver != 0) {
      handleGameOver(socketmessage);
    }
  }
};

const parseChessMove = (moveString) => {
  const temp = moveString.split(',');
  const x = parseInt(temp[0].substring(1), 10);
  const y = parseInt(temp[1].substring(0, temp[1].length - 1), 10);
  return [x, y];
};

const handleRoomCountChangeMessage = (message) => {
  fullscreenLoading.value = false 
  // Render the left information box and Spectator box
  actors.value = message.actors
  setBlacker(message.actors.filter((item) => item.role == 'Joueur Noir')[0])
  setWhiter(message.actors.filter((item) => item.role == 'Joueur Blanc')[0])
};

const handleObserverUpdateMessage = (message) => {
  let num1 = 0,
    num2 = 0
  for (let i = 0; i < 15; i++) {
    for (let j = 0; j < 15; j++) {
      boardStates[i][j] = message.message[i][j]
      if (boardStates[i][j] == 1) {
        num1++
      } else if (boardStates[i][j] == 2) {
        num2++
      }
    }
  }
  currentPlayer.value = num1 == num2 ? 1 : 2
};

window.onbeforeunload = () => {
  playSocket.value.close()
}

const showHover = reactive(
  Array(15)
    .fill()
    .map(() => Array(15).fill(false))
) // Hover preview

const sendMoveToServer = async (x, y) => {
  const json = JSON.stringify({
    type: 4,
    role: playerType.value,
    stepOrder: stepOrder.value,
    gameId: gameId.value,
    message: '(' + x + ',' + y + ')'
  }) // Send the move to the server
  playSocket.value.send(json)
}
const makeMove = (x, y) => {
  if (boardStates[x][y] !== 0 || isWait.value || !isGameing.value) return 

  boardStates[x][y] = currentPlayer.value 
  stepOrder.value++
  isWait.value = true
  
  setTimeout(() => {
    sendMoveToServer(x, y)
  }, 1100)
}
const handleMouseOver = (x, y) => {
  if (x >= 0 && x < 15 && y >= 0 && y < 15) {
    showHover[x][y] = 1
  }
}
const handleMouseLeave = (x, y) => {
  if (x >= 0 && x < 15 && y >= 0 && y < 15) {
    showHover[x][y] = 0
  }
}
const isStarPosition = (x, y) => {
  const starPositions = [
    { x: 3, y: 3 },
    { x: 3, y: 11 },
    { x: 11, y: 3 },
    { x: 11, y: 11 },
    { x: 7, y: 7 }
  ]
  return starPositions.some((position) => position.x === x && position.y === y)
}

const myVisibled = ref(false)
const myMessage = ref('')
const showBlackerMessage = async (message) => {
  addLogList({
    name: actors.value.filter((item) => item.role == 'Joueur Noir')[0].username + '：',
    message: message
  })
  myMessage.value = message
  myVisibled.value = true
  await setTimeout(() => {
    myVisibled.value = false
  }, 5000)
}
const showWhiterMessage = async (message) => {
  addLogList({
    name: actors.value.filter((item) => item.role == 'Joueur Blanc')[0].username + '：',
    message: message
  })
  myMessage.value = message
  myVisibled.value = true
  await setTimeout(() => {
    myVisibled.value = false
  }, 5000)
}

const logList = ref([])
const addLogList = (obj) => {
  logList.value.push(obj)
}
const scroll = ref(null)
watch(logList.value, async () => {
  await nextTick()
  scroll.value.setScrollTop(2000000005)
})

const sendChatMessage = (message) => {
  if (message == '') {
    ElMessage.error('Le message envoyé ne peut pas être vide')
    return
  }
  const json = JSON.stringify({
    type: 3,
    role: playerType.value,
    message: message
  })
  playSocket.value.send(json)

  if (playerType.value == 'Joueur Noir') {
    showBlackerMessage(message)
  } else if (playerType.value == 'Joueur Blanc') {
    showWhiterMessage(message)
  } else {
    // Spectateur
    console.log('name', myInfo.value.username, 'message', message)
    addLogList({
      name: myInfo.value.username,
      message: message
    })
  }
  chatMessage.value = ''
}

// Cancel watching
const noLooking = () => {
  playSocket.value.close()
  verificationCodes.value = ['', '', '', '', ''] // Clear room number input box
  friendDialogVisible.value = true // Open the dialog box for entering the room number
  lookerDialogVisible.value = false // Close the dialog box for selecting whether to watch
}
// Confirm watching
const onLooking = () => {
  const json = JSON.stringify({
    type: '1'
  })
  playSocket.value.send(json)
  playerType.value = 'Spectateur'
  sendChatMessage('Entrez dans la salle')
  isGameing.value = false
  lookerDialogVisible.value = false
}

// Black and white information
const blacker = ref({})
const whiter = ref({})
const setBlacker = (data) => {
  blacker.value = data
}
const setWhiter = (data) => {
  whiter.value = data
}

const onOut = () => {
  if (playSocket.value.url != undefined) playSocket.value.close()
  router.push('/main/game')
}
</script>
<template>
  <div class="background">
    <el-image src="/img/5f645ca6aaab51600412838423.jpg" />
  </div>
  <!-- entrer le numéro de la salle -->
  <div style="position: relative; z-index: 2030">
    <el-dialog
      v-model="friendDialogVisible"
      width="31%"
      center
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <template #title>
        <span style="font-size: 30px">Veuillez entrer le numéro de la salle</span>
      </template>

      <div class="verification-container">
        <input
          v-for="(code, index) in verificationCodes"
          :key="index"
          v-model="verificationCodes[index]"
          @input="handleInput(index, $event)"
          @keydown="handleKeyDown(index, $event)"
          maxlength="1"
          class="verification-input"
        />
      </div>

      <template #footer>
        <span>
          <el-button @click="router.push('/main/game')">Annuler</el-button>
          <el-button type="primary" @click="onGameWithFriend"> Confirmer </el-button>
        </span>
      </template>
    </el-dialog>
  </div>

  <div style="position: relative; z-index: 1">
    <!-- if watching -->
    <el-dialog
      v-model="lookerDialogVisible"
      width="31%"
      center
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <template #title>
        <span style="font-size: 30px">La salle est pleine, vous voulez voir le match ?</span>
      </template>

      <template #footer>
        <span>
          <el-button @click="noLooking">Annuler</el-button>
          <el-button type="primary" @click="onLooking"> Confirmer </el-button>
        </span>
      </template>
    </el-dialog>

    <div style="text-align: center; margin-top: 100px">
      <!-- Spectateur框 -->
      <div class="lookerBox">
        <div v-show="actors.length != 0">Watching</div>
        <el-scrollbar>
          <div style="display: flex">
            <div
              class="lookingItem"
              v-for="item in actors"
              :key="item"
              v-show="item.role == 'Spectateur'"
            >
              <div>
                <div>{{ item.username }}</div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>
    <div
      class="five-container"
      v-loading.fullscreen.lock="fullscreenLoading"
      customClass="loading"
      :element-loading-text="'Attendez que vos amis entrent dans la salle：' + roomId"
    >
      <div style="flex-grow: 1"></div>
      <!-- Information box on the left -->
      <el-card>
        <div style="line-height: 50px">
          <div>
            <!-- noir -->
            <div class="topShow">
              <div>{{ blacker.username }}</div>
            </div>
          </div>
          <div>
            <div class="check">
              <div style="flex-grow: 1"></div>
              <div
                v-show="
                  (isWait && playerType == 'Joueur Blanc') ||
                  (!isWait && playerType == 'Joueur Noir') ||
                  (currentPlayer == 1 && playerType == 'Spectateur')
                "
                style="font-size: 35px; margin-right: 10px"
              >
                =>
              </div>
              <el-image style="width: 50px; height: 50px" :src="blackUrl" />
              <div style="flex-grow: 1"></div>
            </div>
          </div>
          <div style="text-align: center; font-size: 60px">VS</div>
          <div>
            <!-- white -->
            <div class="check">
              <div style="flex-grow: 1"></div>
              <div
                v-show="
                  (isWait && playerType == 'Joueur Noir') ||
                  (!isWait && playerType == 'Joueur Blanc') ||
                  (currentPlayer == 2 && playerType == 'Spectateur')
                "
                style="font-size: 35px; margin-right: 10px"
              >
                =>
              </div>
              <el-image style="width: 50px; height: 50px" :src="whiteUrl" />
              <div style="flex-grow: 1"></div>
            </div>
          </div>
          <div>
            <div class="topShow">
              <div>{{ whiter.username }}</div>
            </div>
          </div>
        </div>
      </el-card>
      <!-- chessboard -->
      <span style="margin-left: 200px">
        <div class="five-board">
          <!-- Traverse every intersection on the board -->
          <div class="board-row" v-for="(row, x) in boardStates" :key="x">
            <!-- Iterate over every intersection in the row -->
            <div
              class="board-cell"
              v-for="(cell, y) in row"
              :key="y"
              @click="() => makeMove(x, y)"
              @mouseover="() => handleMouseOver(x, y)"
              @mouseleave="() => handleMouseLeave(x, y)"
            >
              <!-- star mark -->
              <div v-if="isStarPosition(x, y)" class="star-position"></div>
              <!-- Hover preview -->
              <div
                v-if="showHover[x][y]"
                class="hover-preview"
                :class="currentPlayer == 1 ? 'black-piece' : 'white-piece'"
              ></div>
              <!-- actual chess pieces -->
              <img
                v-if="cell === 1"
                src="/assets/pieces/black.png"
                class="pieces"
                alt="Black Piece"
              />
              <img
                v-if="cell === 2"
                src="/assets/pieces/white.png"
                class="pieces"
                alt="White Piece"
              />
            </div>
          </div>
        </div>
      </span>

      <!-- Information box on the right -->
      <el-card style="margin-left: 90px">
        <div class="infoBox" style="width: 400px">
          <el-card>
            <div class="count">
              <div>
                <span>Nombre de tours</span><span>{{ stepOrder }}</span>
              </div>
              <div>
                <span>Temps de jeu</span><span>{{ formatTime }}</span>
              </div>
            </div>
          </el-card>
          <el-card style="margin-top: 10px">
            <el-table
              height="350"
              style="width: 100%; position: relative"
              empty-text=" "
              :data="logList"
              :show-header="false"
            >
              <el-table-column prop="name" width="140" />
              <el-table-column prop="message" />
            </el-table>
          </el-card>
          <div style="text-align: right; margin-top: 20px">
            <div style="margin-bottom: 10px">
              <el-input
                :disabled="chatMessageInp"
                v-model="chatMessage"
                placeholder="dire quelque chose..."
              >
                <template #append>
                  <el-button type="success" @click="sendChatMessage(chatMessage)">
                    <el-icon>
                      <Upload />
                    </el-icon>
                  </el-button>
                </template>
              </el-input>
            </div>
          </div>
        </div>
      </el-card>
      <div style="flex-grow: 1"></div>
    </div>
  </div>
  <el-popconfirm
    title="Êtes-vous sûr de vouloir quitter?"
    confirm-button-text="Confirmer"
    cancel-button-text="Annuler"
    width="200"
    @confirm="onOut"
  >
    <template #reference>
      <el-button style="position: fixed; right: 160px; bottom: 50px; z-index: 2010" type="danger"
        >quitter</el-button
      >
    </template>
  </el-popconfirm>
</template>

<style scoped>
.verification-container {
  text-align: center;
  width: 100%;
  height: 100%;
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

.background {
  position: fixed;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.lookingItem {
  display: flex;
  border-radius: 5px;
  padding: 10px;
  background-color: white;
  box-shadow: var(--el-box-shadow-light);
}

.lookerBox {
  display: inline-block;
  background-color: white;
  width: 535px;
  height: 120px;
  border-radius: 5px;
  margin-right: 92px;
  margin-bottom: 20px;
  padding: 10px;
  opacity: 0.85;
  box-shadow: var(--el-box-shadow-light);
}

.verification-input {
  border: 1px #6cb2f8 solid;
}

.verification-input > .last-child {
  margin-right: 0;
}

.verification-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 5px #007bff;
}

.five-container {
  display: flex;
}
.el-loading-mask >>> {
  z-index: 2000;
}
.infoBox {
  display: inline-block;
  border-radius: 5px;
}

.topShow {
  width: 200px;
  background-color: #2c3e50;
  color: white;
  padding: 10px;
  line-height: 30px;
  border-radius: 5px;
}

.topShow > div {
  text-align: center;
}


.myMessage {
  font-size: 20px;
}

.check {
  display: flex;
  text-align: center;
  padding: 10px;
}

.count {
  line-height: 25px;
}

.count > div {
  display: flex;
  justify-content: space-between;
}

.five-board {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 620px;
  margin: auto;
  height: 580px;
  background-color: bisque;
  opacity: 1;
}

.board-row {
  display: flex;
  height: 30px;
  margin: 4px;
}

.board-cell {
  width: 35px;
  height: 35px;
  cursor: pointer;
  background-color: bisque;
  opacity: 0.85;
  position: relative;
  /* 重要：为伪元素定位 */
  margin: 3px;
}

/* 如果需要在中间行切分，为该行添加一个特殊的类 */

.hover-preview {
  position: absolute;
  width: 80%;
  /* 预览图像的大小 */
  height: 80%;
  /* 预览图像的大小 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* 将预览图像中心对齐到.board-cell中心 */
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  pointer-events: none;
  opacity: 0.7;
  z-index: 10;
}

.black-piece {
  background-image: url('/assets/pieces/black.png');
}

.white-piece {
  background-image: url('/assets/pieces/white.png');
}

.pieces {
  width: 35px;
}

.star-position {
  position: absolute;
  width: 10px;
  height: 10px;
  background-color: black;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
}

.board-cell::after {
  content: '';
  position: absolute;
  top: 50%;
  left: -5px;
  right: -5px;
  height: 1px;
  background-color: #333;
}

.board-cell::before,
.board-cell::after {
  z-index: 99;
}

.board-cell img {
  position: absolute;
  z-index: 100;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.board-cell::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -4px;
  bottom: -4px;
  width: 1px;
  background-color: #333;
}

.five-board::before,
.five-board::after {
  content: '';
  position: absolute;
  background-color: #333;
  z-index: 1;
}

.five-board::before {
  top: 0;
  right: 0;
  left: 0;
  height: 1px;
}

.five-board::after {
  bottom: 0;
  right: 0;
  left: 0;
  height: 1px;
}

.board-row::before,
.board-row::after {
  content: '';
  position: absolute;
  background-color: #333;
  z-index: 1;
}

.board-row::before {
  top: 0;
  bottom: 0;
  left: 0;
  width: 1px;
}

.board-row::after {
  top: 0;
  bottom: 0;
  right: 0;
  width: 1px;
}
</style>
