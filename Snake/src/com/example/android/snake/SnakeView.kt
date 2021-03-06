package com.example.android.snake

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
//import com.example.android.snake.SnakeView.Coordinate
import com.example.android.snake.static.*
import java.util.ArrayList
import android.graphics.drawable.Drawable

/**
* User: Natalia.Ukhorskaya
*/

public class SnakeView(val myContext: Context, val myAttrs: AttributeSet): TileView(myContext, myAttrs)  {

    {
        initSnakeView()
    }

    private val mRedrawHandler = RefreshHandler()
    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200

    fun initSnakeView() {
        setFocusable(true)

        val r = this.getContext()?.getResources()!!

        resetTiles(4)
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar)!!)
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar)!!)
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar)!!)

    }

    fun initNewGame() {
        mSnakeTrail.clear()
        mAppleList.clear()

        mSnakeTrail.add(Coordinate(7, 7))
        mSnakeTrail.add(Coordinate(6, 7))
        mSnakeTrail.add(Coordinate(5, 7))
        mSnakeTrail.add(Coordinate(4, 7))
        mSnakeTrail.add(Coordinate(3, 7))
        mSnakeTrail.add(Coordinate(2, 7))
        mNextDirection = NORTH

        addRandomApple()
        addRandomApple()

        mMoveDelay = 300
        mScore = 0
    }

    fun coordArrayListToArray(coordinatesArrayList: ArrayList<Coordinate>): IntArray? {
        var count = coordinatesArrayList.size() - 1
        val coordinatesArray = IntArray(count * 2)
        for (index in 0..count) {
            val c: Coordinate = coordinatesArrayList.get(index)
            coordinatesArray[2 * index] = c.x
            coordinatesArray[2 * index + 1] = c.y
        }
        return coordinatesArray
    }

    public fun saveState(): Bundle {
        val map = Bundle()

        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList))
        map.putInt("mDirection", Integer.valueOf(mDirection)!!)
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection)!!)
        map.putLong("mMoveDelay", mMoveDelay.toLong())
        map.putLong("mScore", mScore.toLong())
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail))

        return map
    }

    fun coordArrayToArrayList(coordinatesArray: IntArray): ArrayList<Coordinate> {
        val coordinatesArrayList = ArrayList<Coordinate>()

        val count = coordinatesArray.size
        for (index in 0..count step 2) {
            val c = Coordinate(coordinatesArray[index]!!, coordinatesArray[index + 1]!!)
            coordinatesArrayList.add(c)
        }
        return coordinatesArrayList
    }

    public fun restoreState(icicle: Bundle) {
        setMode(PAUSE)

        mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList")!!)
        mDirection = icicle.getInt("mDirection")
        mNextDirection = icicle.getInt("mNextDirection")
        mMoveDelay = icicle.getLong("mMoveDelay")
        mScore = icicle.getLong("mScore")
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail")!!)
    }

    public override fun onKeyDown(keyCode: Int, msg: KeyEvent?): Boolean {

        when(keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                setDirection("UP")
                return (true)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                setDirection("DOWN")
                return (true)
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                setDirection("LEFT")
                return (true)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                setDirection("RIGHT")
                return (true)
            }
            else -> {
            }
        }

        return super<TileView>.onKeyDown(keyCode, msg)
    }

    public fun setTextView(newView: TextView) {
        mStatusText = newView
    }

    public fun setDirection(direction: String) {
        when (direction) {
            "UP" -> {
                if (mDirection != SOUTH) {
                    mNextDirection = NORTH
                }
            }
            "DOWN" -> {
                if (mDirection != NORTH) {
                    mNextDirection = SOUTH
                }
            }
            "LEFT" -> {
                if (mDirection != EAST) {
                    mNextDirection = WEST
                }
            }
            "RIGHT" -> {
                if (mDirection != WEST) {
                    mNextDirection = EAST
                }
            }
            else -> {
                Log.e("error", "Incorrect direction")
            }
        }
    }

    fun setBackgroundImage(draw: Int) {
        val resources = myContext.getResources()
        this.setBackgroundDrawable(resources?.getDrawable(draw))
    }

    public fun maybeStart() {
        if ((mMode == READY).or(mMode == LOSE)) {
            initNewGame()
            setMode(RUNNING)
            update()
        }

        if (mMode == PAUSE) {
            setMode(RUNNING)
            update()
        }
    }

    public fun setMode(newMode: Int) {
        val oldMode = mMode
        mMode = newMode

        if (newMode == RUNNING && oldMode != RUNNING) {
            mStatusText?.setVisibility(View.INVISIBLE)
            update()
            return
        }

        val res = getContext()?.getResources()!!
        var str: CharSequence = ""
        when (newMode) {
            PAUSE -> {
                str = res.getText(R.string.mode_pause)!!
            }
            READY -> {
                str = res.getText(R.string.mode_ready)!!
            }
            LOSE -> {
                str = res.getString(R.string.mode_lose_prefix) + mScore + res.getString(R.string.mode_lose_suffix)
            }
            else -> {
            }
        }

        mStatusText?.setText(str)
        mStatusText?.setVisibility(View.VISIBLE)
    }

    fun addRandomApple() {
        var newCoord: Coordinate? = null
        var found: Boolean = false
        while (!found) {
            // Choose a new location for our apple
            var newX = 1 + RNG.nextInt(mXTileCount - 2)
            var newY = 1 + RNG.nextInt(mYTileCount - 2)
            newCoord = Coordinate(newX, newY)

            // Make sure it's not already under the snake
            var collision = false
            val snakelength = mSnakeTrail.size() - 1
            for (index in 0..snakelength) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true
                }
            }
            found = !collision
        }
        if (newCoord == null) {
            Log.e("SnakeView", "Somehow ended up with a null newCoord!")
        }
        mAppleList.add(newCoord!!)
    }

    public fun update() {
        if (mMode == RUNNING) {
            val  now: Long = System.currentTimeMillis()
            if (now.minus(mLastMove) > mMoveDelay) {
                clearTiles()
                updateWalls()
                updateSnake()
                updateApples()
                mLastMove = now
            }
            mRedrawHandler.sleep(mMoveDelay)
        }
    }

    fun updateWalls() {
        for (x in 0..mXTileCount - 1) {
            setTile(GREEN_STAR, x, 0)
            setTile(GREEN_STAR, x, mYTileCount - 1)
        }
        for (y in 1..mYTileCount - 1) {
            setTile(GREEN_STAR, 0, y)
            setTile(GREEN_STAR, mXTileCount - 1, y)
        }
    }

    fun updateApples() {
        for (c in mAppleList) {
            setTile(YELLOW_STAR, c.x, c.y)
        }
    }

    fun updateSnake() {
        var growSnake = false

        // grab the snake by the head
        val head = mSnakeTrail.get(0)
        var newHead = Coordinate(1, 1)

        mDirection = mNextDirection

        when (mDirection) {
            EAST -> {
                newHead = Coordinate(head.x + 1, head.y)
            }
            WEST -> {
                newHead = Coordinate(head.x - 1, head.y)
            }
            NORTH -> {
                newHead = Coordinate(head.x, head.y - 1)
            }
            SOUTH-> {
                newHead = Coordinate(head.x, head.y + 1)
            }
            else -> {

            }
        }

        if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
                            || (newHead.y > mYTileCount - 2)) {
            setMode(LOSE)
            return
        }

        val snakelength = mSnakeTrail.size() - 1
        for (snakeindex in 0..snakelength) {
            val c = mSnakeTrail.get(snakeindex)
            if (c.equals(newHead)) {
                setMode(LOSE)
                return
            }
        }

        // Look for apples
        val applecount = mAppleList.size() - 1
        for (appleindex in  0..applecount) {
            val c = mAppleList.get(appleindex)
            if (c.equals(newHead)) {
                mAppleList.remove(c)
                addRandomApple()

                mScore++
                if (mMoveDelay >= 20) {
                    mMoveDelay -= 20
                }
                growSnake = true
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(0, newHead)
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.remove(mSnakeTrail.size() - 1)
        }

        var index = 0
        for (c in mSnakeTrail) {
            if (index == 0) {
                setTile(YELLOW_STAR, c.x, c.y)
            } else {
                setTile(RED_STAR, c.x, c.y)
            }
            index++
        }

    }

    class Coordinate(val x: Int, val y: Int) {
        override fun equals(other: Any?): Boolean {
            return other is Coordinate && x == other.x && y == other.y
        }

        override fun toString(): String {
            return "Coordinate: [" + x + "," + y + "]"
        }
    }

    public inner class RefreshHandler(): Handler() {

        override fun handleMessage(msg: Message?) {
            this@SnakeView.update()
            this@SnakeView.invalidate()
        }

        public fun sleep(delayMillis: Long) {
            sendMessageDelayed(obtainMessage(0), delayMillis)
        }
    }

}

