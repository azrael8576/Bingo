package com.alex.bingo.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alex.bingo.Bean.Member;
import com.alex.bingo.R;
import com.alex.bingo.Bean.Room;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth auth;
    private TextView nickText;
    private ImageView avatar;
    private Group groupAvatar;
    int[] avatars = {
            R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
    };
    private Member member;
    private FirebaseRecyclerAdapter<Room, RoomHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText roomEdit = new EditText( MainActivity.this);
                roomEdit.setText("Welcome");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.room_name)
                        .setMessage(R.string.enter_room_info)
                        .setView(roomEdit)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomName = roomEdit.getText().toString();
                                Room room = new Room(roomName,member);
                                DatabaseReference rooms = FirebaseDatabase.getInstance().getReference("rooms");
                                DatabaseReference roomRef =rooms.push();
                                roomRef.setValue(room);
                                String key =roomRef.getKey();
                                Log.d(TAG, "onClick: " + key);
                                roomRef.child("id").setValue(key);

                                //開局者進入Bingo
                                Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                                bingo.putExtra("ROOM_ID",key);
                                bingo.putExtra("IS_CREATOR",true);
                                startActivity(bingo);
                            }
                        })
                        .show();
            }
        });
        findViews();
        auth = FirebaseAuth.getInstance();
//        CrashTest();
    }

    private void CrashTest() {
        Button crashButton = new Button(this);
        crashButton.setText("Crash!");
        crashButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Crashlytics.getInstance().crash(); // Force a crash
            }
        });

        addContentView(crashButton, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void findViews() {
        nickText = findViewById(R.id.nickname);
        avatar = findViewById(R.id.avatar);
        groupAvatar = findViewById(R.id.group_avatars);
        groupAvatar.setVisibility(View.GONE);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean visible = groupAvatar.getVisibility() == View.GONE ? false : true;
                groupAvatar.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);

        //RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query query = FirebaseDatabase.getInstance().getReference("rooms").limitToLast(30);
        FirebaseRecyclerOptions<Room> options = new FirebaseRecyclerOptions.Builder<Room>()
                .setQuery(query,Room.class)
                .build();
        adapter = new FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull final Room room) {
                holder.image.setImageResource(avatars[room.getInit().getAvatarId()]);
                holder.text.setText(room.getTitle());
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: ");
                        Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                        bingo.putExtra("ROOM_ID",room.getId());
                        bingo.putExtra("IS_CREATOR",false);
                        startActivity(bingo);
                    }
                });
            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.room_row,viewGroup,false);
                return new RoomHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }


    public class RoomHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView text;
        public RoomHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.room_image);
            text = itemView.findViewById(R.id.room_text);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Firebase auth
        auth.addAuthStateListener(this);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //remove auth
        auth.removeAuthStateListener(this);
        adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_settings:
                return true;
            case R.id.action_signout:
                auth.signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        Log.d(TAG, "onAuthStateChanged: ");
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            final String displayName = user.getDisplayName();
            String uid = user.getUid();
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("displayName")
                    .setValue(displayName);
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("uid")
                    .setValue(uid);
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            member = (Member) dataSnapshot.getValue(Member.class);
                            if (member.getNickname() == null){
                                showNicknameDialog(displayName);
                            }
                            else {
                                nickText.setText(member.getNickname());
                            }
                            avatar.setImageResource(avatars[member.getAvatarId()]);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
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
                            .setAvailableProviders(Arrays.asList(
                                   new AuthUI.IdpConfig.EmailBuilder().build(),
                                   new AuthUI.IdpConfig.GoogleBuilder().build()
                            ))
                            .setIsSmartLockEnabled(false)
                    .build()
                    ,RC_SIGN_IN);
        }
    }

    private void showNicknameDialog(String displayName) {
        final EditText nickEdit = new EditText(this);
        nickEdit.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle(R.string.nick_name)
                .setMessage(R.string.nick_name_info)
                .setView(nickEdit)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickname = nickEdit.getText().toString();
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(auth.getUid())
                                .child("nickname")
                                .setValue(nickname);
                    }
                })
                .show();
    }
    public void changeNickname(View view){
        showNicknameDialog(nickText.getText().toString());
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ImageView){
            int selectedAvatarId = 0;
            switch (v.getId()){
                case R.id.avatar_1:
                    selectedAvatarId = 1;
                    break;
                case R.id.avatar_2:
                    selectedAvatarId = 2;
                    break;
                case R.id.avatar_3:
                    selectedAvatarId = 3;
                    break;
                case R.id.avatar_4:
                    selectedAvatarId = 4;
                    break;
                case R.id.avatar_5:
                    selectedAvatarId = 5;
                    break;
                case R.id.avatar_6:
                    selectedAvatarId = 6;
                    break;
            }
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getCurrentUser().getUid())
                    .child("avatarId")
                    .setValue(selectedAvatarId);
            groupAvatar.setVisibility(View.GONE);
        }
    }
}
