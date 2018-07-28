$(document).ready(function() {
    $('#userList').dataTable({
        language: {
            url: '/auth/localization/zh-cn.json'
        },
        rowReorder: {
            selector: 'td:nth-child(2)'
        },
        responsive: true,
        processing: true,
        serverSide: true,
        ajax: "/auth/userList",
        searching: false,
        ordering: false,
        columns: [
            {
                class: "title",
                render: function (data, type, row, meta) {
                    return data;
                }
            },
            {
                render: function (data, type, row, meta) {
                    return data;
                }
            },
            {
                data: null,
                render: function (data, type, row, meta) {
                    return "<a class='delete' href='#'>删除账户</a>";
                }
            }
        ],
        createdRow: function (row, data, dataIndex) {
            $(".delete", row).on("click", function () {
                var title = $(".title", $(this).parent().parent()).html();
                window.location.href = "delete/"+title;
            });
        }
    });
    
    $("#home").on("click", function () {
        window.location.href = '.';
    });
    $('#changepwd').on("click", function () {
        window.location.href = 'change';
    });
    $('#register').on("click", function () {
        window.location.href = 'register';
    });
    $('#logoff').on("click", function () {
        window.location.href = 'logout';
    });
});