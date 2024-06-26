package project.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id @GeneratedValue
    @Column(name = "MEBER_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PASSWORD")
    private Long pw;

    @Column(name = "POINT")
    private int pt = 0;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Board> boards = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Reply> replies = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "sendMember", cascade = CascadeType.ALL)
    @Column(name = "send_member_id")
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiveMember", cascade = CascadeType.ALL)
    @Column(name = "receive_member_id")
    private List<Message> receivedMessages = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<BoardLike> likes = new ArrayList<>();

    public Member(String name, String email, Long pw) {
        this.name = name;
        this.email = email;
        this.pw = pw;
    }
}
