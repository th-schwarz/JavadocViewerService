package codes.thischwa.jdvs.web;

import codes.thischwa.jdvs.repository.GitRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
  private final GitRepositoryRepository repository;

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("repos", repository.findAll());
    return "index";
  }
}
