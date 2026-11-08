// Published page: fills the table from /catalog. Values from the server are only ever set as
// text, never as HTML.

const rows = document.getElementById('rows');

function cell(text, className) {
    const td = document.createElement('td');
    td.textContent = text;
    if (className) td.className = className;
    return td;
}

function message(text, className) {
    const tr = document.createElement('tr');
    const td = cell(text, className);
    td.colSpan = 3;
    tr.append(td);
    rows.replaceChildren(tr);
}

fetch('/catalog')
    .then(response => {
        if (!response.ok) throw new Error('HTTP ' + response.status);
        return response.json();
    })
    .then(data => {
        const items = data.items || [];
        if (!items.length) { message('Nothing is published yet.', 'empty'); return; }
        rows.replaceChildren(...items.map(item => {
            const tr = document.createElement('tr');
            const hash = cell(item.sha256.slice(0, 16) + '…', 'mono hash');
            hash.title = item.sha256;
            tr.append(cell(item.app), cell(item.ver, 'mono'), hash);
            return tr;
        }));
    })
    .catch(e => message('Could not load the catalogue: ' + e.message, 'error'));
