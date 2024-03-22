<script setup>

import { ref, reactive, watch } from 'vue'
import { getUserInfo, addAIChess, sendChessDetail } from '@/api/gameAI.js'
import { useUserIderStore } from '@/stores/userIder.js'
import { ElMessage } from 'element-plus'
import router from '@/router'
import 'animate.css'
const boardStates = reactive(
  Array(15)
    .fill()
    .map(() => Array(15).fill(0))
)

const stepOrder = ref(1) // 当前步数
const gameId = ref(15) // 游戏ID，从后端获取
const isWait = ref(false) // 是否等待AI落子
const currentPlayer = ref(1) // 1 代表黑棋, 2 代表白棋
const userIderStore = useUserIderStore()
const id = userIderStore.userInfo.id // 获取用户id
const difficulty = router.currentRoute.value.query.difficulty
console.log(difficulty)
const myInfo = ref({})
const blackUrl = ref('/assets/pieces/black.png')
const whiteUrl = ref('/assets/pieces/white.png')
const nameAI = ref('')

const isHighLight = ref(
  Array(15)
    .fill()
    .map(() => Array(15).fill(0))
) // 悬停状态数组// 当前是否白子高亮，维护一个数组

const init = async () => {
  // 新增AI棋局
  var res = await addAIChess()
  gameId.value = res.data
  // 获取用户信息
  res = await getUserInfo(id)
  myInfo.value = res.data
  // AI信息
  if (difficulty == 1) {
    nameAI.value = '小将AI'
  } else if (difficulty == 2) {
    nameAI.value = '中将AI'
  } else {
    nameAI.value = '将军AI'
  }
}
init()

import { onMounted, onUnmounted, computed } from 'vue'
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
startTimer()


const isGameing = ref(true)
const showHover = reactive(
  Array(15)
    .fill()
    .map(() => Array(15).fill(false))
) // 悬停状态数组

const highlightX = ref(0)
const highlightY = ref(0)

// 向服务端发送消息
const sendMoveToServer = async (x, y) => {
  let level
  level = difficulty - 1
  const chessData = {
    playerMoves: { x, y },
    stepOrder: stepOrder.value,
    level: level, // 假设难度级别1 2 3
    gameId: gameId.value,
    boardStates
  } // 组装发送数据
  const response = await sendChessDetail(chessData)
  const { aipieces, gameStatus} = response.data // 拦截器拦截了，解一层就可以
  if (aipieces) {
    // 处理 AI 落子
    const { x, y } = aipieces
    boardStates[x][y] = 2 // AI 落子
    isHighLight.value[highlightX.value][highlightY.value] = 0 //清除之前的高亮的
    isHighLight.value[x][y] = 1 // 当前白子高亮
    highlightX.value = x // 记录当前白子高亮的坐标
    highlightY.value = y
    // stepOrder.value++
  }

  if (gameStatus != 3) {
    // 游戏结束
    animatedText.value = getWinterText(gameStatus)
    stopTimer()
    showAnimation()
    var res = await getUserInfo(id)
    myInfo.value = res.data
    isGameing.value = false
    // console.log('game over!!')
    // 执行跳转或显示动画的逻辑
  }

  isWait.value = false // 服务器返回成功
}
const playChess = ref(null)
const makeMove = (x, y) => {
  // 更新棋盘状态的逻辑
  if (boardStates[x][y] !== 0 || isWait.value || !isGameing.value) return // 下过棋或者在等待人机下棋都不能再次下棋了
  isWait.value = true // 等待服务器返回
  boardStates[x][y] = 1 // 玩家落子
  stepOrder.value++
  setTimeout(() => {
    sendMoveToServer(x, y)
  }, 1100)
}
const handleMouseOver = (x, y) => {
  // console.log('鼠标悬停', x, y)
  if (x >= 0 && x < 15 && y >= 0 && y < 15) {
    showHover[x][y] = 1
  }
}
const handleMouseLeave = (x, y) => {
  // console.log('鼠标离开', x, y)
  if (x >= 0 && x < 15 && y >= 0 && y < 15) {
    showHover[x][y] = 0
  }
}
const isStarPosition = (x, y) => {
  // 定义星位的坐标
  const starPositions = [
    { x: 3, y: 3 },
    { x: 3, y: 11 },
    { x: 11, y: 3 },
    { x: 11, y: 11 },
    { x: 7, y: 7 }
    // 如果棋盘足够大，还可以添加中心点 { x: 7, y: 7 }
  ]
  // 检查当前坐标是否是星位
  return starPositions.some((position) => position.x === x && position.y === y)
}
const getWinterText = (status) => {
  if (status == 0) return userIderStore.userInfo.nickname + ' 获胜'
  if (status == 1) return nameAI.value + ' 获胜'
  if (status == 2) return '旗鼓相当的对手'
}

// 再一次人机对战的困难选择
const selectDifficultyDialogVisible = ref(false)
const difficultyTitle = ref('Veuillez choisir une difficulté')
const newDifficulty = ref(-1)
const imgUrl = ref('/img/thinking.png')

watch(newDifficulty, (newValue) => {
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
const confirmDifficulty = () => {
  // 确定难度，开始人机对战
  if (newDifficulty.value === -1) {
    ElMessage.error('请选择难度')
  } else {
    // 开始人机对战
    selectDifficultyDialogVisible.value = false
    // 谁教你用go的啊哥，谁家的go跳转啊,push跳转也不行，必须刷新页面。
    // 应该在那个监听器里面搞，但是搞的话还会出现选择的时候下面的状态都重置了，再回来就回不来了。
    // 最佳实践应该是点击跳转一个新路由，别让他在这个路由里了
    // router.go({ path: '/boardAI', query: { difficulty: newDifficulty.value } })
    router.push({ path: '/boardAI', query: { difficulty: newDifficulty.value } })
  }
}

const animatedText = ref('')
const animatedPicture = ref(null)
//动画方法
const animatedPictureUrl = '/img/devil.png'
const showAnimation = async () => {
  animatedPicture.value.style['display'] = 'block'
  animatedPicture.value.classList.add('animate__animated', 'animate__bounceInLeft')
  await setTimeout(() => {
    animatedPicture.value.style['display'] = 'none'
  }, 5000)
}
onMounted(() => {
  startTimer()
})
onUnmounted(() => {
  stopTimer()
})


</script>
<template>
  <div
    class="app"
    style="width: 100vw; height: 100vh"
  >
    <div class="background">
      <el-image src="/img/5f645d2d6e8bc1600412973308.jpg" />
    </div>
    <div style="position: relative; z-index: 1">
      <span ref="animatedPicture" class="animate">
        <div><img :src="animatedPictureUrl" /></div>
        <div style="font-weight: 900; font-size: 30px">{{ animatedText }}</div>
      </span>

      <div>
        <!-- 选择困难对话框 -->
        <el-dialog v-model="selectDifficultyDialogVisible" width="30%" center>
          <template #title>
            <span style="font-size: 30px">人机对战</span>
          </template>
          <span style="text-align: center">
            <div style="font-size: 30px">{{ difficultyTitle }}</div>
            <div style="display: flex; justify-content: space-around; padding: 30px 50px">
              <el-button style="width: 150px" size="large" @click="newDifficulty = 1"
                >简单</el-button
              >
              <el-button style="width: 150px" size="large" type="warning" @click="newDifficulty = 2"
                >普通</el-button
              >
              <el-button style="width: 150px" size="large" type="danger" @click="newDifficulty = 3"
                >困难</el-button
              >
            </div>
            <div style="text-align: center; margin-right: 10px">
              <el-image style="width: 100px; height: 100px" :src="imgUrl" />
            </div>
          </span>
          <template #footer>
            <span>
              <el-button size="large" @click="selectDifficultyDialogVisible = false"
                >取消</el-button
              >
              <el-button type="primary" size="large" @click="confirmDifficulty"> 确定 </el-button>
            </span>
          </template>
        </el-dialog>

        <div class="gomoku-container">
          <div style="flex-grow: 1"></div>
          <el-card>
            <div class="infoBox">
              <div class="topShow">
                <div>{{ nameAI }}</div>
              </div>
              <div class="check">
                <el-image style="width: 50px; height: 50px" :src="whiteUrl" />
              </div>
              <el-card>
                <div class="count">
                  <div>
                    <span>回合数</span><span>{{ stepOrder }}</span>
                  </div>
                  <div>
                    <span>游戏时间</span><span>{{ formatTime }}</span>
                  </div>
                </div>
              </el-card>
            </div>
          </el-card>
          <!-- 棋盘 -->
          <span style="margin-left: 200px">
            <div class="gomoku-board">
              <!-- 遍历棋盘上的每个交叉点 -->
              <div class="board-row" v-for="(row, x) in boardStates" :key="x">
                <!-- 遍历行中的每个交叉点 -->
                <div
                  class="board-cell"
                  v-for="(cell, y) in row"
                  :key="y"
                  @click="() => makeMove(x, y)"
                  @mouseover="() => handleMouseOver(x, y)"
                  @mouseleave="() => handleMouseLeave(x, y)"
                >
                  <!-- 星位的标记 -->
                  <div v-if="isStarPosition(x, y)" class="star-position"></div>
                  <!-- 悬停预览 -->
                  <div
                    v-if="showHover[x][y]"
                    class="hover-preview"
                    :class="currentPlayer == 1 ? 'black-piece' : 'white-piece'"
                  ></div>
                  <!-- 实际棋子 -->
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
                    :class="isHighLight[x][y] == 1 ? 'highlight' : ''"
                  />
                  <!-- 悬停预览、实际棋子等 -->
                </div>
              </div>
            </div>
          </span>
          <el-card style="margin-left: 200px">
            <div class="infoBox">
              <div class="topShow">
                <div>{{ myInfo.username }}</div>
              </div>
              <div class="check">
                <el-image style="width: 50px; height: 50px" :src="blackUrl"/>
              </div>
              <!-- 底部信息 -->
              <el-card>
                <div class="count">
                  <div>
                    <span>得分</span><span>{{ myInfo.userScore }}</span>
                  </div>
                  <div>
                    <span>总局数</span><span>{{ myInfo.gameTotalCounts }}</span>
                  </div>
                  <div>
                    <span>好友对战局数</span><span>{{ myInfo.gamePersonCounts }}</span>
                  </div>
                  <div>
                    <span>人机对战局数</span><span>{{ myInfo.gameAiCounts }}</span>
                  </div>
                  <div>
                    <span>获胜局数</span><span>{{ myInfo.gameSuccessCounts }}</span>
                  </div>
                  <div>
                    <span>失败局数</span><span>{{ myInfo.gameFailCounts }}</span>
                  </div>
                  <div>
                    <span>平局数</span><span>{{ myInfo.gameDeadHeatCounts }}</span>
                  </div>
                </div>
              </el-card>
              <div style="text-align: right; padding-top: 20px">
                <el-button @click="selectDifficultyDialogVisible = true">再来一次</el-button>
                <el-popconfirm
                  title="你确定要退出吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  width="200"
                  @confirm="router.push('/main/game')"
                >
                  <template #reference>
                    <el-button type="danger">退出</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </el-card>

          <div style="flex-grow: 1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.highlight {
  border: 2px solid #8b4513; /* 棕色 */
  /* box-shadow: 0 0 10px #888888; */
  /* border-radius: 5px; 调整圆角大小 */

  /* border-image: linear-gradient(to right, red, orange) 1; */
}
.background {
  position: fixed;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.animate {
  width: 300px;
  height: 300px;
  position: absolute;
  text-align: center;
  left: 805px;
  top: 300px;
  z-index: 10000000;
  display: none;
}

.gomoku-container {
  display: flex;
  padding-top: 145px;
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

.check {
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

.gomoku-board {
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
  /* flex: 1; */
  width: 35px;
  height: 35px;
  cursor: pointer;
  background-color: bisque;
  position: relative;
  opacity: 0.85;
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
  opacity: 1;
}

.white-piece {
  background-image: url('/assets/pieces/white.png');
  opacity: 1;
}

.pieces {
  width: 35px;
}

.star-position {
  position: absolute;
  width: 10px;
  /* 星位的大小 */
  height: 10px;
  background-color: black;
  /* 星位的颜色 */
  border-radius: 50%;
  /* 使其成为圆点 */
  /* 将星位定位到交叉点的中心 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* 确保星位在线条之上 */
  z-index: 2;
}

.board-cell::after {
  content: '';
  position: absolute;
  top: 50%;
  left: -5px;
  /* 向左延伸10px */
  right: -5px;
  /* 向右延伸10px */
  height: 1px;
  /* 线条宽度 */
  background-color: #333;
  /* 线条颜色 */
}

.board-cell::before,
.board-cell::after {
  /* 其他样式保持不变 */
  z-index: 99;
  /* 将线条置于背景之下 */
}

.board-cell img {
  position: absolute;
  z-index: 100;
  /* 确保棋子在线条之上 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.board-cell::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -4px;
  /* 向上延伸10px */
  bottom: -4px;
  /* 向下延伸10px */
  width: 1px;
  /* 线条宽度 */
  background-color: #333;
  /* 线条颜色 */
}

.gomoku-board::before,
.gomoku-board::after {
  content: '';
  position: absolute;
  background-color: #333;
  /* 边界线条的颜色 */
  z-index: 1;
}

/* 棋盘顶部和底部的边界线 */
.gomoku-board::before {
  top: 0;
  right: 0;
  left: 0;
  height: 1px;
  /* 边界线条的厚度 */
}

.gomoku-board::after {
  bottom: 0;
  right: 0;
  left: 0;
  height: 1px;
  /* 边界线条的厚度 */
}

/* 棋盘左侧和右侧的边界线 */
/* 为了不与已有的::before和::after伪元素冲突，我们可以选择添加到.board-row */
.board-row::before,
.board-row::after {
  content: '';
  position: absolute;
  background-color: #333;
  /* 边界线条的颜色 */
  z-index: 1;
}

.board-row::before {
  top: 0;
  bottom: 0;
  left: 0;
  width: 1px;
  /* 边界线条的厚度 */
}

.board-row::after {
  top: 0;
  bottom: 0;
  right: 0;
  width: 1px;
  /* 边界线条的厚度 */
}
</style>
