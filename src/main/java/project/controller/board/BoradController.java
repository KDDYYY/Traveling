package project.controller.board;


import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.domain.Board;
import project.domain.Image;
import project.domain.Member;
import project.domain.Reply;
import project.service.BoardService;
import project.service.FileUploadService;
import project.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BoradController {

    private final BoardService boardService;
    private final MemberService memberService;

    @GetMapping("/home")
    public String main(Model model, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size){

        List<Board> boards = boardService.findPaginatedBoards(page, size);
        //List<Board> boards = boardService.findBoards();

        model.addAttribute("boards", boards);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        return "practice/home";
    }

    //게시물 등록
    @GetMapping("/boards/new")
    public String createBoard(Model model, HttpSession session) {
        Member sessionMember = (Member) session.getAttribute("member");

        if (sessionMember != null) {
            model.addAttribute("BoardForm", new BoardForm());
            return "practice/board/board";
        } else {
            return "redirect:/members/login";
        }
    }



    // 게시물 등록 및 파일 업로드
    @PostMapping("/boards/new")
    public String create(@Valid BoardForm boardForm, HttpSession session,
                         @RequestParam("files") List<MultipartFile> files) {
        try {
            Member sessionMember = (Member) session.getAttribute("member");

            if (sessionMember == null) {
                return "redirect:/members/login";
            }

            Member member = memberService.findOne(sessionMember.getId());

            // 게시물 저장
            Board board = new Board(boardForm.getTitle(), boardForm.getContent(), LocalDateTime.now());

            if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
                boardService.saveBoard(board, member, files); //이미지가 포함 될 때
            } else {
                boardService.saveBoard(board, member); //이미지가 포함되지 않을 때
            }

            memberService.increaseBoardMemberPoint(member.getId()); // 포인트 올리기

            return "redirect:/home";
        } catch (Exception e) {
            return "redirect:/members/login";
        }
    }


    // 게시물 조회
    @GetMapping("/boards/{id}")
    public String viewBoard(@PathVariable Long id, Model model, HttpSession session) {
        Board board = boardService.increaseBoardClick(id);
        List<Reply> replies = boardService.findReplies(id);
        List<Image> images = boardService.findImagesByBoardId(id);

        // 좋아요 개수 조회
        long likeCount = boardService.getLikeCountForBoard(board);

        // 좋아요 여부 확인
        boolean isLiked = false;
        Member sessionMember = (Member) session.getAttribute("member");
        if (sessionMember != null) {
            Member member = memberService.findByEmail(sessionMember.getEmail());
            isLiked = boardService.isLikedByMember(member, board);
        }

        model.addAttribute("board", board);
        model.addAttribute("ReplyForm", new ReplyForm());
        model.addAttribute("replies", replies);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("isLiked", isLiked);

        if (images != null && !images.isEmpty()) {
            model.addAttribute("images", images);
        }

        return "practice/board/boardView";
    }



    // 좋아요 추가 또는 취소
    @PostMapping("/boards/{id}/like")
    public String toggleLike(@PathVariable Long id, HttpSession session) {
        Member sessionMember = (Member) session.getAttribute("member");

        if(sessionMember == null)
            return "redirect:/members/login";

        Member member = memberService.findByEmail(sessionMember.getEmail());
        Board board = boardService.findOne(id);

        if (boardService.isLikedByMember(member, board)) {
            // 이미 좋아요를 누른 상태이면 좋아요 취소
            boardService.removeBoardLike(member, board);
        } else {
            // 좋아요를 누르지 않은 상태이면 좋아요 추가
            boardService.saveBoardLike(member, board);
        }

        return "redirect:/boards/" + id;
    }

    //게시물 수정
    @GetMapping("/boards/{id}/modify")
    public String modifyBoard(@PathVariable Long boardId, Model model, HttpSession session){
        Member sessionMember = (Member) session.getAttribute("member");

        Member member = memberService.findByEmail(sessionMember.getEmail());
        Board board = boardService.findOne(boardId);

        if(member.getId() != board.getMember().getId()){
            return "practice/board";
        }

        model.addAttribute("board", board);
        model.addAttribute("boardForm", new BoardForm());

        return "practice/board/modify";
        }

    //게시물 수정
    @PostMapping("/boards/{id}/modify")
    public String modifyBoard(@PathVariable Long id, @Valid BoardForm boardForm, HttpSession session,
                              @RequestParam("files") List<MultipartFile> files){
        try {
            Member sessionMember = (Member) session.getAttribute("member");

            if (sessionMember == null) {
                return "redirect:/members/login";
            }

            Member member = memberService.findByEmail(sessionMember.getEmail());
            Board existingBoard = boardService.findOne(id);

            if (!member.getId().equals(existingBoard.getMember().getId())) {
                return "practice/board/board";
            }

            // 게시물 수정
            existingBoard.setTitle(boardForm.getTitle());
            existingBoard.setContent(boardForm.getContent());

            if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
                boardService.modifyBoard(existingBoard, member, files); //이미지가 포함 될 때
            } else {
                boardService.modifyBoard(existingBoard, member); //이미지가 포함되지 않을 때
            }

            return "redirect:/home";
        }catch (Exception e){
            return "redirect:/members/login";
        }
    }



    //댓글 기능
    @PostMapping("/boards/{id}/reply")
    public String setReply(@ModelAttribute ReplyForm replyForm , HttpSession session, @PathVariable Long id){
        Member sessionMember = (Member) session.getAttribute("member");
        Member member = memberService.findByEmail(sessionMember.getEmail());

        Board board = boardService.findOne(id);
        Reply reply = new Reply(replyForm.getContent(), replyForm.getRate());

        boardService.saveReply(reply, board, member);
        memberService.increaseReplyMemberPoint(member.getId());

        return "redirect:/boards/" + id;
}

//검색 기능
@GetMapping("/boards/search")
    public String searchBoards(@RequestParam("keyword")String keyword, @RequestParam("searchOption") String searchOption, Model model){

    List<Board> searchResults;

    if ("title".equals(searchOption)) {
        searchResults = boardService.findTitleSearchBoards(keyword);
    } else if ("content".equals(searchOption)) {
        searchResults = boardService.findContentSearchBoards(keyword);
    } else if ("titleAndContent".equals(searchOption)) {
        searchResults = boardService.findContentTitleSearchBoards(keyword);
    } else {
        searchResults = boardService.findTitleSearchBoards(keyword);
    }

    model.addAttribute("searchResults", searchResults);
    return "practice/board/searchList";
}

//조회순 정렬
    @GetMapping("/boards/click")
    public String searchClickBoards(Model model){
        List<Board> clickResults = boardService.findClickBoards();
        model.addAttribute("clickResults", clickResults);
        return "practice/board/clickList";
    }

    //즐겨찾기 기능
    @PostMapping("/boards/favorite")
    public String addFavorite(@RequestParam("boardId") Long boardId, HttpSession session) {
        try {
            Member sessionMember = (Member) session.getAttribute("member");
            Board board = boardService.findOne(boardId);

            Member member = memberService.findByEmail(sessionMember.getEmail()); // 세션에서 가져온 Member를 영속 상태로 가져옴

            boardService.addFavorite(member, board);

            return "redirect:/home";
        } catch (Exception e) {
            return "practice/home";
        }
    }

    //즐겨찾기 목록
    @GetMapping("/members/favorites")
    public String listFavorite(HttpSession session, Model model){
        Member sessionMember = (Member) session.getAttribute("member");

        List<Board> favorList = boardService.favoriteList(sessionMember.getId());
        model.addAttribute("favorList", favorList);
        return "practice/favorList";
    }


}
