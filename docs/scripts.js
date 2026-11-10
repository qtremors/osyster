// Osyster website. Fades sections in, and fills the GitHub numbers, the latest version, and the
// direct APK link from the GitHub API. Without it the page still works: the buttons open the
// latest release page and the numbers stay as dashes.

(() => {
    const sections = document.querySelectorAll('.reveal');
    const showAll = () => sections.forEach(section => section.classList.add('seen'));
    if (!('IntersectionObserver' in window)) return showAll();

    const watcher = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('seen');
                watcher.unobserve(entry.target);
            }
        });
    }, { rootMargin: '0px 0px -8% 0px' });
    sections.forEach(section => watcher.observe(section));

    // Background tabs and slow phones can delay the observer; never leave a section blank.
    setTimeout(showAll, 2500);
})();

(async () => {
    const api = 'https://api.github.com/repos/qtremors/osyster';
    const format = value => new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value);
    const set = (id, value) => {
        const element = document.getElementById(id);
        if (element) element.textContent = value;
    };

    try {
        const [repo, latest] = await Promise.all([
            fetch(api).then(response => response.ok ? response.json() : null),
            fetch(`${api}/releases/latest`).then(response => response.ok ? response.json() : null)
        ]);

        if (repo) {
            set('gh-stars', format(repo.stargazers_count));
            set('gh-forks', format(repo.forks_count));
        }

        if (latest) {
            // Show the latest version on the buttons and link straight to its APK.
            const assets = latest.assets || [];
            const apk = assets.find(asset => asset.name.endsWith('.apk'));
            document.querySelectorAll('.release').forEach(label => { label.textContent = `Download ${latest.tag_name}`; });
            if (apk) document.querySelectorAll('.apk-link').forEach(link => { link.href = apk.browser_download_url; });
            document.querySelectorAll('.version-note').forEach(note => { note.textContent = `Latest: ${latest.tag_name}. `; });
            set('gh-latest', format(assets.reduce((sum, asset) => sum + asset.download_count, 0)));
        }

        // Downloads across every release, page by page.
        let total = 0;
        let page = `${api}/releases?per_page=100`;
        while (page) {
            const response = await fetch(page);
            if (!response.ok) break;
            (await response.json()).forEach(release => release.assets.forEach(asset => { total += asset.download_count; }));
            const next = /<([^>]+)>;\s*rel="next"/.exec(response.headers.get('Link') || '');
            page = next ? next[1] : null;
        }
        if (total > 0) set('gh-total', format(total));
    } catch {
        // Offline or rate limited: the links still point at the releases page.
    }
})();
