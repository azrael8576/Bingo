package com.alex.bingo.activity

import android.content.Intent
import android.os.Bundle
import androidx.constraintlayout.widget.Group
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

import com.alex.bingo.bean.Member
import com.alex.bingo.R
import com.alex.bingo.bean.Room
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import java.util.Arrays

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    companion object {

        private val TAG = MainActivity::class.java.simpleName
        private const val RC_SIGN_IN = 100
    }
    private var auth: FirebaseAuth? = null
    private var nickText: TextView? = null
    private var avatar: ImageView? = null
    private var groupAvatar: Group? = null
    internal var avatars = intArrayOf(R.drawable.avatar_0, R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6)
    private var member: Member? = null
    private var adapter: FirebaseRecyclerAdapter<Room, RoomHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            val roomEdit = EditText(this@MainActivity)
            roomEdit.setText("Welcome")
            AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.room_name)
                    .setMessage(R.string.enter_room_info)
                    .setView(roomEdit)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val roomName = roomEdit.text.toString()
                        val room = Room(roomName, member!!)
                        val rooms = FirebaseDatabase.getInstance().getReference("rooms")
                        val roomRef = rooms.push()
                        roomRef.setValue(room)
                        val key = roomRef.key
                        Log.d(TAG, "onClick: " + key!!)
                        roomRef.child("id").setValue(key)

                        //開局者進入Bingo
                        val bingo = Intent(this@MainActivity, BingoActivity::class.java)
                        bingo.putExtra("ROOM_ID", key)
                        bingo.putExtra("IS_CREATOR", true)
                        startActivity(bingo)
                    }
                    .show()
        }
        findViews()
        auth = FirebaseAuth.getInstance()
        //        CrashTest();
    }

    private fun CrashTest() {
        val crashButton = Button(this)
        crashButton.text = "Crash!"
        crashButton.setOnClickListener {
            Crashlytics.getInstance().crash() // Force a crash
        }

        addContentView(crashButton, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun findViews() {
        nickText = findViewById(R.id.nickname)
        avatar = findViewById(R.id.avatar)
        groupAvatar = findViewById(R.id.group_avatars)
        groupAvatar!!.visibility = View.GONE
        avatar!!.setOnClickListener {
            val visible = groupAvatar!!.visibility != View.GONE
            groupAvatar!!.visibility = if (visible) View.GONE else View.VISIBLE
        }
        findViewById<View>(R.id.avatar_0).setOnClickListener(this)
        findViewById<View>(R.id.avatar_1).setOnClickListener(this)
        findViewById<View>(R.id.avatar_2).setOnClickListener(this)
        findViewById<View>(R.id.avatar_3).setOnClickListener(this)
        findViewById<View>(R.id.avatar_4).setOnClickListener(this)
        findViewById<View>(R.id.avatar_5).setOnClickListener(this)
        findViewById<View>(R.id.avatar_6).setOnClickListener(this)

        //RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val query = FirebaseDatabase.getInstance().getReference("rooms").limitToLast(30)
        val options = FirebaseRecyclerOptions.Builder<Room>()
                .setQuery(query, Room::class.java)
                .build()
        adapter = object : FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            override fun onBindViewHolder(holder: RoomHolder, position: Int, room: Room) {
                holder.image.setImageResource(avatars[room.init!!.avatarId])
                holder.text.text = room.title
                holder.itemView.setOnClickListener {
                    Log.d(TAG, "onClick: ")
                    val bingo = Intent(this@MainActivity, BingoActivity::class.java)
                    bingo.putExtra("ROOM_ID", room.id)
                    bingo.putExtra("IS_CREATOR", false)
                    startActivity(bingo)
                }
            }

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RoomHolder {
                val view = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.room_row, viewGroup, false)
                return RoomHolder(view)
            }
        }
        recyclerView.adapter = adapter
    }


    inner class RoomHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var image: ImageView = itemView.findViewById(R.id.room_image)
        internal var text: TextView = itemView.findViewById(R.id.room_text)

    }


    override fun onStart() {
        super.onStart()
        //Firebase auth
        auth!!.addAuthStateListener(this)
        adapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()
        //remove auth
        auth!!.removeAuthStateListener(this)
        adapter!!.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            R.id.action_signout -> auth!!.signOut()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
        Log.d(TAG, "onAuthStateChanged: ")
        val user = firebaseAuth.currentUser

        if (user != null) {
            val displayName = user.displayName
            val uid = user.uid
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("displayName")
                    .setValue(displayName)
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("uid")
                    .setValue(uid)
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            member = dataSnapshot.getValue(Member::class.java)
                            if (member!!.nickname == null) {
                                showNicknameDialog(displayName)
                            } else {
                                nickText!!.text = member!!.nickname
                            }
                            avatar!!.setImageResource(avatars[member!!.avatarId])
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
            //            //get nickname
            //            FirebaseDatabase.getInstance()
            //                    .getReference("users")
            //                    .child(uid)
            //                    .child("nickname")
            //                    .addListenerForSingleValueEvent(new ValueEventListener() {
            //                        @Override
            //                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            //                            if (dataSnapshot.getValue() != null) {
            //                                String nickname = (String) dataSnapshot.getValue();
            //                            }
            //                            else {
            //                                showNicknameDialog(displayName);
            //                            }
            //                        }
            //
            //                        @Override
            //                        public void onCancelled(@NonNull DatabaseError databaseError) {
            //
            //                        }
            //                    });
        } else {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(Arrays.asList<AuthUI.IdpConfig>(
                                    AuthUI.IdpConfig.EmailBuilder().build(),
                                    AuthUI.IdpConfig.GoogleBuilder().build()
                            ) as List<AuthUI.IdpConfig>)
                            .setIsSmartLockEnabled(false)
                            .build(), RC_SIGN_IN)
        }
    }

    private fun showNicknameDialog(displayName: String?) {
        val nickEdit = EditText(this)
        nickEdit.setText(displayName)
        AlertDialog.Builder(this)
                .setTitle(R.string.nick_name)
                .setMessage(R.string.nick_name_info)
                .setView(nickEdit)
                .setPositiveButton(R.string.ok) { dialog, which ->
                    val nickname = nickEdit.text.toString()
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(auth!!.uid!!)
                            .child("nickname")
                            .setValue(nickname)
                }
                .show()
    }

    fun changeNickname(view: View) {
        showNicknameDialog(nickText!!.text.toString())
    }

    override fun onClick(v: View) {
        if (v is ImageView) {
            var selectedAvatarId = 0
            when (v.getId()) {
                R.id.avatar_1 -> selectedAvatarId = 1
                R.id.avatar_2 -> selectedAvatarId = 2
                R.id.avatar_3 -> selectedAvatarId = 3
                R.id.avatar_4 -> selectedAvatarId = 4
                R.id.avatar_5 -> selectedAvatarId = 5
                R.id.avatar_6 -> selectedAvatarId = 6
            }
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth!!.currentUser!!.uid)
                    .child("avatarId")
                    .setValue(selectedAvatarId)
            groupAvatar!!.visibility = View.GONE
        }
    }

}
