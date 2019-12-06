package com.alex.bingo.activity

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.alex.bingo.bean.NumberButton
import com.alex.bingo.R
import com.alex.bingo.bean.Room
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import java.util.ArrayList
import java.util.HashMap

class BingoActivity : AppCompatActivity() {
    companion object {
        private const val NUMBER_COUNT = 25
        private val TAG = BingoActivity::class.java.simpleName
    }
    private var roomId: String? = null
    private var recyclerView: RecyclerView? = null
    private var info: TextView? = null
    var isCreator: Boolean = false
    private var randomNumbers: MutableList<Int>? = null
    private var adapter: FirebaseRecyclerAdapter<Boolean, NumberHolder>? = null
    private var buttons: MutableList<NumberButton>? = null
    private val numberMap = HashMap<Int, NumberButton>()
    private var myTurn = false

    private val statusListener: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val status = dataSnapshot.value as Long
            when (status.toInt()) {
                Room.STATUS_CREATED -> info!!.text = getString(R.string.waiting_player)
                Room.STATUS_JOINED -> {
                    info!!.text = getString(R.string.opponent_join)
                    isMyTurn = isCreator
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId!!)
                            .child("status")
                            .setValue(Room.STATUS_CREATOR_TURN)
                }
                Room.STATUS_CREATOR_TURN -> isMyTurn = isCreator
                Room.STATUS_JOINERS_TURN -> isMyTurn = !isCreator
                Room.STATUS_CREATOR_BINGO -> if (!isCreator) {
                    AlertDialog.Builder(this@BingoActivity)
                            .setTitle(R.string.loser)
                            .setMessage(R.string.you_lose)
                            .setPositiveButton(R.string.ok) { dialog, which ->
                                //TODO: clean room
                                endGame()
                            }.show()
                }
                Room.STATUS_JOINERS_BINGO -> if (isCreator) {
                    AlertDialog.Builder(this@BingoActivity)
                            .setTitle(R.string.loser)
                            .setMessage(R.string.you_lose)
                            .setPositiveButton(R.string.ok) { dialog, which ->
                                //TODO: clean room
                                endGame()
                            }.show()
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    var isMyTurn: Boolean
        get() = myTurn
        set(myTurn) {
            this.myTurn = myTurn
            info!!.text = if (myTurn) getString(R.string.select_number) else getString(R.string.waitting_opponent)
        }

    private fun endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId!!)
                .child("status")
                .removeEventListener(statusListener)
        if (isCreator) {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId!!)
                    .removeValue()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)
        findViews()
        roomId = intent.getStringExtra("ROOM_ID")
        isCreator = intent.getBooleanExtra("IS_CREATOR", false)
        generateRandomNumbers()

        if (isCreator) {
            //fill firebase room numbers
            for (i in 0 until NUMBER_COUNT) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomId!!)
                        .child("numbers")
                        .child((i + 1).toString() + "")
                        .setValue(false)
            }
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId!!)
                    .child("status")
                    .setValue(Room.STATUS_CREATED)
        } else { //for joiner
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId!!)
                    .child("status")
                    .setValue(Room.STATUS_JOINED)
        }
        //Recycler view
        val query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId!!)
                .child("numbers")
                .orderByKey()
        val options = FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query, Boolean::class.java).build()
        adapter = object : FirebaseRecyclerAdapter<Boolean, NumberHolder>(options) {
            override fun onBindViewHolder(holder: NumberHolder, position: Int, model: Boolean) {
                holder.button.text = buttons!![position].number.toString() + ""
                holder.button.isEnabled = !buttons!![position].isPicked
                holder.button.setOnClickListener {
                    if (isMyTurn) {
                        val number = buttons!![position].number
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId!!)
                                .child("numbers")
                                .child(number.toString() + "")
                                .setValue(true)
                    }
                }
            }

            override fun onChildChanged(type: ChangeEventType, snapshot: DataSnapshot, newIndex: Int, oldIndex: Int) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                Log.d(TAG, "onChildChanged: " + type.name + "/" + snapshot.key)
                if (type == ChangeEventType.CHANGED) {
                    val numberButton = numberMap[Integer.parseInt(snapshot.key!!)]
                    val pos = numberButton!!.position
                    val holder = recyclerView!!.findViewHolderForAdapterPosition(pos) as NumberHolder?
                    holder!!.button.isEnabled = false
                    numberButton.isPicked = true
                    if (isMyTurn) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId!!)
                                .child("status")
                                .setValue(if (isCreator) Room.STATUS_JOINERS_TURN else Room.STATUS_CREATOR_TURN)
                        //Bingo
                        val nums = IntArray(NUMBER_COUNT)
                        for (i in 0 until NUMBER_COUNT) {
                            nums[i] = if (buttons!![i].isPicked) 1 else 0
                        }
                        var bingo = 0
                        var sum: Int
                        for (i in 0..4) {
                            sum = 0
                            for (j in 0..4) {
                                sum += nums[i * 5 + j]
                            }
                            bingo += if (sum == 5) 1 else 0
                            sum = 0
                            for (j in 0..4) {
                                sum += nums[j * 5 + i]
                            }
                            bingo += if (sum == 5) 1 else 0
                        }
                        Log.d(TAG, "onChildChanged:bingo $bingo")
                        if (bingo > 0) {
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId!!)
                                    .child("status")
                                    .setValue(if (isCreator) Room.STATUS_CREATOR_TURN else Room.STATUS_JOINERS_TURN)
                            AlertDialog.Builder(this@BingoActivity)
                                    .setTitle(R.string.bingo)
                                    .setMessage(R.string.bingo_info)
                                    .setPositiveButton(R.string.ok) { _, _ ->
                                        //TODO: clean room
                                        endGame()
                                    }.show()
                        }
                    }
                }
            }

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): NumberHolder {
                val view = LayoutInflater.from(this@BingoActivity).inflate(R.layout.single_number, viewGroup, false)
                return NumberHolder(view)
            }
        }
        recyclerView!!.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter!!.startListening()
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId!!)
                .child("status")
                .addValueEventListener(statusListener)
    }

    override fun onStop() {
        super.onStop()
        adapter!!.stopListening()
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId!!)
                .child("status")
                .removeEventListener(statusListener)
        if (isCreator) {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId!!)
                    .removeValue()
        }
        finish()
    }

    internal inner class NumberHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var button: NumberButton = itemView.findViewById(R.id.number)

    }

    private fun generateRandomNumbers() {
        randomNumbers = ArrayList()
        for (i in 0 until NUMBER_COUNT) {
            randomNumbers!!.add(i + 1)
        }
        (randomNumbers as ArrayList<Int>).shuffle()
        buttons = ArrayList()
        for (i in 0 until NUMBER_COUNT) {
            val button = NumberButton(this)
            button.text = randomNumbers!![i].toString() + ""
            button.number = randomNumbers!![i]
            button.position = i
            buttons!!.add(button)
            numberMap[button.number] = button
        }
    }

    private fun findViews() {
        recyclerView = findViewById(R.id.recycler)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = GridLayoutManager(this, 5)
        info = findViewById(R.id.info)
    }

}
