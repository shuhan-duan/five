package com.five.pojo.pojo;

import lombok.Data;

@Data
public  class GameCoordinate {
        private int x;
        private int y;
        private int score;

        public GameCoordinate(int x, int y) {
            this.x = x;
            this.y = y;
            this.score = 0;
        }

        public GameCoordinate(int score, int x, int y) {
            this.score = score;
            this.x = x;
            this.y = y;
        }

    }