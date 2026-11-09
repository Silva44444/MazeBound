(() => {
  const links = document.querySelectorAll("[data-page]");
  const pages = document.querySelectorAll(".page");
  const title = document.getElementById("page-title");
  const titles = {
    home: "Overview",
    exploration: "My exploration",
    leaderboards: "Leaderboards",
    profile: "Player profile"
  };

  function showPage(name) {
    if (!titles[name]) name = "home";
    pages.forEach(page => page.classList.toggle("active", page.id === "page-" + name));
    links.forEach(link => {
      const active = link.dataset.page === name;
      link.classList.toggle("active", active);
      if (active) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });
    title.textContent = titles[name];
  }

  links.forEach(link => {
    link.addEventListener("click", event => {
      event.preventDefault();
      const name = link.dataset.page;
      history.replaceState(null, "", "#" + name);
      showPage(name);
    });
  });

  showPage(location.hash.slice(1) || "home");
})();